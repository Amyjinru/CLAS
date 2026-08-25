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
import java.time.LocalDateTime;
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

    public UserService(UserMapper userMapper, UserRoleMapper userRoleMapper, VerificationCodeStore verificationCodeStore,
                       BCryptPasswordEncoder passwordEncoder, com.clas.common.JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.verificationCodeStore = verificationCodeStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        User user = userMapper.selectById(phone);
        if (user == null) throw new BusinessException("手机号或密码错误");
        String storedPassword = user.getPassword();
        boolean passwordMatches = storedPassword != null && storedPassword.startsWith("$2")
            ? passwordEncoder.matches(request.password(), storedPassword)
            : request.password().equals(storedPassword);
        if (passwordMatches && (storedPassword == null || !storedPassword.startsWith("$2"))) {
            user.setPassword(passwordEncoder.encode(request.password()));
            userMapper.updateById(user);
        }
        if (!passwordMatches) throw new BusinessException("手机号或密码错误");
        if (Boolean.FALSE.equals(user.getEnabled())) throw new BusinessException("账号已被禁用，请联系管理员");
        return loginResponse(user, defaultRole(phone));
    }

    public LoginResponse register(RegisterRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        PasswordValidator.validate(request.password());
        if (!request.password().equals(request.confirmPassword())) throw new BusinessException("两次输入的密码不一致");
        verificationCodeStore.verify(phone, "register", request.code());
        if (request.role() != null && !request.role().isBlank() && !"USER".equalsIgnoreCase(request.role().trim())) {
            throw new BusinessException("公开注册仅支持普通用户账号");
        }
        if (userMapper.selectById(phone) != null) throw new BusinessException("手机号已存在");
        User user = new User();
        user.setPhone(phone);
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        userMapper.insert(user);
        grantRole(phone, "USER");
        return loginResponse(user, "USER");
    }

    public void sendRegisterCode(SendCodeRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        if (userMapper.selectById(phone) != null) throw new BusinessException("该手机号已被注册");
        verificationCodeStore.generateAndStore(phone, "register");
    }

    public void sendForgotPasswordCode(SendCodeRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        User user = userMapper.selectById(phone);
        if (user == null) throw new BusinessException("该手机号未绑定任何账号");
        if (Boolean.FALSE.equals(user.getEnabled())) throw new BusinessException("该账号已被禁用，无法重置密码");
        verificationCodeStore.generateAndStore(phone, "forgot");
    }

    public LoginResponse resetForgotPassword(ResetPasswordRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        PasswordValidator.validate(request.newPassword());
        if (!request.newPassword().equals(request.confirmPassword())) throw new BusinessException("两次输入的密码不一致");
        verificationCodeStore.verify(phone, "forgot", request.code());
        User user = userMapper.selectById(phone);
        if (user == null) throw new BusinessException("该手机号未绑定任何账号");
        if (Boolean.FALSE.equals(user.getEnabled())) throw new BusinessException("账号已被禁用，无法重置密码");
        String oldPassword = user.getPassword();
        boolean samePassword = oldPassword != null && oldPassword.startsWith("$2")
            ? passwordEncoder.matches(request.newPassword(), oldPassword)
            : request.newPassword().equals(oldPassword);
        if (samePassword) throw new BusinessException("新密码不能与旧密码相同");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        return loginResponse(user, defaultRole(phone));
    }

    public LoginResponse switchRole(String phone, String requestedRole) {
        String role = requestedRole == null ? "" : requestedRole.trim().toUpperCase();
        User user = userMapper.selectById(phone);
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) throw new BusinessException("账号已被禁用或不存在");
        UserRole identity = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
            .eq(UserRole::getUserId, phone).eq(UserRole::getRole, role));
        boolean legacyIdentity = identity == null && role.equals(user.getRole());
        if (!legacyIdentity && (identity == null || !"APPROVED".equals(identity.getStatus()))) {
            throw new BusinessException("该身份尚未审核通过或已不可用");
        }
        return loginResponse(user, role);
    }

    public List<String> rolesOf(String userId) {
        List<String> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)).stream()
            .filter(identity -> "APPROVED".equals(identity.getStatus()))
            .map(UserRole::getRole).toList();
        User legacy = userMapper.selectById(userId);
        if (roles.isEmpty() && legacy != null) return java.util.stream.Stream.of("USER", legacy.getRole()).distinct().toList();
        return roles.contains("USER") ? roles : java.util.stream.Stream.concat(java.util.stream.Stream.of("USER"), roles.stream()).toList();
    }

    public void grantRole(String userId, String role) {
        UserRole existing = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId).eq(UserRole::getRole, role));
        if (existing == null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRole(role);
            userRole.setStatus("APPROVED");
            userRole.setCreatedAt(LocalDateTime.now());
            userRole.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.insert(userRole);
        }
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
