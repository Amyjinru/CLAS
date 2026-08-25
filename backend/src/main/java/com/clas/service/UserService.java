package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.PasswordValidator;
import com.clas.common.PhoneValidator;
import com.clas.common.VerificationCodeStore;
import com.clas.dto.LoginRequest;
import com.clas.dto.LoginResponse;
import com.clas.dto.RegisterRequest;
import com.clas.dto.ResetPasswordRequest;
import com.clas.dto.SendCodeRequest;
import com.clas.entity.User;
import com.clas.entity.UserRole;
import com.clas.mapper.UserMapper;
import com.clas.mapper.UserRoleMapper;
import com.clas.dto.SwitchRoleRequest;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final VerificationCodeStore verificationCodeStore;
    private final BCryptPasswordEncoder passwordEncoder;
    private final com.clas.common.JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, UserRoleMapper userRoleMapper, VerificationCodeStore verificationCodeStore, BCryptPasswordEncoder passwordEncoder, com.clas.common.JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.verificationCodeStore = verificationCodeStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        User user = userMapper.selectById(phone);
        if (user == null) {
            throw new BusinessException("手机号或密码错误");
        }

        // 渐进式密码迁移：BCrypt 哈希以 $2 开头（兼容 $2a$/$2b$/$2x$/$2y$）
        String storedPassword = user.getPassword();
        boolean passwordMatches;
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            passwordMatches = passwordEncoder.matches(request.password(), storedPassword);
        } else {
            // 旧明文密码 —— 验证通过后自动升级为 BCrypt
            passwordMatches = request.password().equals(storedPassword);
            if (passwordMatches) {
                user.setPassword(passwordEncoder.encode(request.password()));
                userMapper.updateById(user);
            }
        }

        if (!passwordMatches) {
            throw new BusinessException("手机号或密码错误");
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        return loginResponse(user, defaultRole(user.getPhone()));
    }

    public LoginResponse register(RegisterRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        PasswordValidator.validate(request.password());
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 1. 验证码校验（失败会抛异常）
        verificationCodeStore.verify(phone, "register", request.code());

        // 2. 公开注册只能生成普通用户；商家、骑手、管理员均由受控流程授予。
        if (request.role() != null
            && !request.role().isBlank()
            && !"USER".equalsIgnoreCase(request.role().trim())) {
            throw new BusinessException("公开注册仅支持普通用户账号");
        }

        // 3. 手机号唯一性
        if (userMapper.selectById(phone) != null) {
            throw new BusinessException("手机号已存在");
        }

        User user = new User();
        user.setPhone(phone);
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        userMapper.insert(user);
        grantRole(phone, "USER");
        return loginResponse(user, defaultRole(user.getPhone()));
    }

    /**
     * 注册 — 发送验证码。
     * 检查手机号未被注册，生成验证码并存入 VerificationCodeStore。
     */
    public void sendRegisterCode(SendCodeRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        // 检查手机号是否已被注册
        if (userMapper.selectById(phone) != null) {
            throw new BusinessException("该手机号已被注册");
        }
        verificationCodeStore.generateAndStore(phone, "register");
    }

    /**
     * 忘记密码 — 发送验证码。
     * 验证手机号已绑定到某个账号，生成验证码并存入 VerificationCodeStore。
     */
    public void sendForgotPasswordCode(SendCodeRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        // 检查手机号是否已绑定到某个账号
        User user = userMapper.selectById(phone);
        if (user == null) {
            throw new BusinessException("该手机号未绑定任何账号");
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new BusinessException("该账号已被禁用，无法重置密码");
        }
        // 生成并存储验证码（冷却检查在 store 内部处理）
        verificationCodeStore.generateAndStore(phone, "forgot");
    }

    /**
     * 忘记密码 — 验证码校验 + 密码重置 + 自动登录。
     */
    public LoginResponse resetForgotPassword(ResetPasswordRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        PasswordValidator.validate(request.newPassword());
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        // 1. 验证码校验（失败会抛异常）
        verificationCodeStore.verify(phone, "forgot", request.code());
        // 2. 查找用户
        User user = userMapper.selectById(phone);
        if (user == null) {
            throw new BusinessException("该手机号未绑定任何账号");
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new BusinessException("账号已被禁用，无法重置密码");
        }
        // 3. 新密码不能与旧密码相同（兼容明文和 BCrypt）
        String oldPassword = user.getPassword();
        boolean samePassword;
        if (oldPassword != null && oldPassword.startsWith("$2")) {
            samePassword = passwordEncoder.matches(request.newPassword(), oldPassword);
        } else {
            samePassword = request.newPassword().equals(oldPassword);
        }
        if (samePassword) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        return loginResponse(user, defaultRole(user.getPhone()));
    }

    public LoginResponse switchRole(String userId, SwitchRoleRequest request) {
        String targetRole = request.role().trim().toUpperCase();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (!rolesOf(userId).contains(targetRole)) throw new BusinessException("尚未获得该身份，无法切换");
        return loginResponse(user, targetRole);
    }

    public List<String> rolesOf(String userId) {
        List<String> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
            .eq(UserRole::getUserId, userId)).stream().map(UserRole::getRole).toList();
        User legacy = userMapper.selectById(userId);
        if (roles.isEmpty() && legacy != null) {
            return java.util.stream.Stream.of("USER", legacy.getRole()).distinct().toList();
        }
        return roles.contains("USER") ? roles : java.util.stream.Stream.concat(java.util.stream.Stream.of("USER"), roles.stream()).toList();
    }

    public void grantRole(String userId, String role) {
        boolean exists = userRoleMapper.exists(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId).eq(UserRole::getRole, role));
        if (!exists) { UserRole userRole = new UserRole(); userRole.setUserId(userId); userRole.setRole(role); userRoleMapper.insert(userRole); }
    }

    private LoginResponse loginResponse(User user, String activeRole) {
        List<String> roles = rolesOf(user.getPhone());
        user.setRole(roles.contains(activeRole) ? activeRole : "USER");
        user.setRoles(roles);
        user.setPassword(null);
        return new LoginResponse(user, jwtUtil.generateToken(user.getPhone(), user.getRole()), roles);
    }

    private String defaultRole(String userId) {
        List<String> roles = rolesOf(userId);
        if (roles.contains("ADMIN")) return "ADMIN";
        if (roles.contains("MERCHANT")) return "MERCHANT";
        if (roles.contains("RIDER")) return "RIDER";
        return "USER";
    }
}
