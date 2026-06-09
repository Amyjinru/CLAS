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
import com.clas.mapper.UserMapper;
import java.util.Set;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    // 第一版只允许这三种演示角色，避免注册接口写入不一致的角色字符串。
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "MERCHANT", "ADMIN");

    private final UserMapper userMapper;
    private final VerificationCodeStore verificationCodeStore;
    private final BCryptPasswordEncoder passwordEncoder;
    private final com.clas.common.JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, VerificationCodeStore verificationCodeStore, BCryptPasswordEncoder passwordEncoder, com.clas.common.JwtUtil jwtUtil) {
        this.userMapper = userMapper;
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
        user.setPassword(null);
        String token = jwtUtil.generateToken(user.getPhone(), user.getRole());
        return new LoginResponse(user, token);
    }

    public LoginResponse register(RegisterRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        PasswordValidator.validate(request.password());
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 1. 验证码校验（失败会抛异常）
        verificationCodeStore.verify(phone, "register", request.code());

        // 2. 空角色统一落到普通用户
        String role = request.role() == null || request.role().isBlank() ? "USER" : request.role().trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new BusinessException("角色只能是 USER、MERCHANT 或 ADMIN");
        }

        // 3. 手机号唯一性
        if (userMapper.selectById(phone) != null) {
            throw new BusinessException("手机号已存在");
        }

        User user = new User();
        user.setPhone(phone);
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        userMapper.insert(user);
        user.setPassword(null);
        String token = jwtUtil.generateToken(user.getPhone(), user.getRole());
        return new LoginResponse(user, token);
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
        user.setPassword(null);
        // 5. 返回登录信息（自动登录）
        String token = jwtUtil.generateToken(user.getPhone(), user.getRole());
        return new LoginResponse(user, token);
    }
}
