package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.PasswordValidator;
import com.clas.common.PhoneValidator;
import com.clas.common.VerificationCodeStore;
import com.clas.dto.DemoLoginRequest;
import com.clas.dto.DemoAccessRequest;
import com.clas.dto.LoginRequest;
import com.clas.dto.LoginNoticeResponse;
import com.clas.dto.LoginResponse;
import com.clas.dto.RegisterRequest;
import com.clas.dto.ResetPasswordRequest;
import com.clas.dto.SendCodeRequest;
import com.clas.entity.User;
import com.clas.entity.UserPenalty;
import com.clas.entity.UserRole;
import com.clas.mapper.UserMapper;
import com.clas.mapper.UserRoleMapper;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final int ACTIVE_SESSION_MINUTES = 3;
    private static final int LOGIN_CHALLENGE_MINUTES = 10;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final VerificationCodeStore verificationCodeStore;
    private final BCryptPasswordEncoder passwordEncoder;
    private final com.clas.common.JwtUtil jwtUtil;
    private final PenaltyService penaltyService;

    @Value("${clas.demo-accounts.enabled:false}")
    private boolean demoAccountsEnabled;

    @Value("${clas.demo-accounts.access-password:}")
    private String demoAccessPassword;

    public UserService(UserMapper userMapper, UserRoleMapper userRoleMapper, VerificationCodeStore verificationCodeStore,
                       BCryptPasswordEncoder passwordEncoder, com.clas.common.JwtUtil jwtUtil,
                       PenaltyService penaltyService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.verificationCodeStore = verificationCodeStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.penaltyService = penaltyService;
    }

    public LoginResponse login(LoginRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        UserPenalty activeBan = penaltyService.activeAccountBan(phone);
        if (activeBan != null) {
            throw accountBanned(activeBan);
        }
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
        if (hasActiveSession(user) && !isSameDevice(user, request.deviceId())) {
            if (request.code() == null || request.code().isBlank()) {
                userMapper.createLoginChallenge(phone, user.getSessionToken(), UUID.randomUUID().toString(),
                    normalizeDeviceId(request.deviceId()), LocalDateTime.now());
                throw new BusinessException(409, "账号已在其他设备登录，请使用手机验证码确认登录", "LOGIN_VERIFICATION_REQUIRED");
            }
            verificationCodeStore.verify(phone, "login-session", request.code());
        }
        List<String> roles = rolesOf(phone, user);
        return loginResponse(user, defaultRole(roles), request.deviceId(), roles);
    }

    /**
     * 快捷登录仅在显式启用演示配置后可用。
     * 已存在有效会话时仍要求验证码，保证与常规登录一致的单设备保护。
     */
    public LoginResponse demoLogin(DemoLoginRequest request) {
        verifyDemoAccess(request.accessPassword());
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        UserPenalty activeBan = penaltyService.activeAccountBan(phone);
        if (activeBan != null) {
            throw accountBanned(activeBan);
        }
        User user = userMapper.selectById(phone);
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException("演示账号不存在或已被禁用");
        }
        if (hasActiveSession(user) && !isSameDevice(user, request.deviceId())) {
            if (request.code() == null || request.code().isBlank()) {
                userMapper.createLoginChallenge(phone, user.getSessionToken(), UUID.randomUUID().toString(),
                    normalizeDeviceId(request.deviceId()), LocalDateTime.now());
                throw new BusinessException(409, "账号已在其他设备登录，请使用手机验证码确认登录", "LOGIN_VERIFICATION_REQUIRED");
            }
            verificationCodeStore.verify(phone, "login-session", request.code());
        }
        List<String> roles = rolesOf(phone, user);
        return loginResponse(user, defaultRole(roles), request.deviceId(), roles);
    }

    public void verifyDemoAccess(DemoAccessRequest request) {
        verifyDemoAccess(request.password());
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
        return loginResponse(user, "USER", null, List.of("USER"));
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

    public void sendLoginCode(SendCodeRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        User user = userMapper.selectById(phone);
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException("账号已被禁用或不存在");
        }
        if (!hasActiveSession(user)) {
            throw new BusinessException("当前账号没有其他有效登录会话");
        }
        verificationCodeStore.generateAndStore(phone, "login-session");
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
        List<String> roles = rolesOf(phone, user);
        return loginResponse(user, defaultRole(roles), null, roles);
    }

    public LoginResponse switchRole(String phone, String requestedRole) {
        String role = requestedRole == null ? "" : requestedRole.trim().toUpperCase();
        User user = userMapper.selectById(phone);
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) throw new BusinessException("账号已被禁用或不存在");
        List<String> roles = rolesOf(phone, user);
        if (!roles.contains(role)) {
            throw new BusinessException("该身份尚未审核通过或已不可用");
        }
        return loginResponse(user, role, user.getSessionDeviceId(), roles);
    }

    /**
     * 退出登录：清除当前会话，使其它设备无需验证码即可再次登录。
     * 仅当 token 中的 sessionToken 仍与库中一致时才清除，避免误清新设备的会话。
     */
    public void logout(String phone, String sessionToken) {
        if (phone == null || phone.isBlank() || sessionToken == null || sessionToken.isBlank()) return;
        userMapper.clearSessionToken(phone, sessionToken);
    }

    /** 仅对仍处于当前会话的请求刷新活跃时间，避免轮询请求把旧会话长期判定为在线。 */
    public void touchActiveSession(User user) {
        if (user == null || user.getSessionToken() == null || user.getSessionToken().isBlank()) return;
        LocalDateTime lastSeenAt = user.getSessionLastSeenAt();
        if (lastSeenAt == null || lastSeenAt.isBefore(LocalDateTime.now().minusSeconds(30))) {
            userMapper.touchSession(user.getPhone(), user.getSessionToken(), LocalDateTime.now());
        }
    }

    /** 供当前在线设备轮询；验证码确认前即可看到另一台设备的登录请求。 */
    public LoginNoticeResponse getPendingLoginNotice(User user) {
        if (user == null || user.getPendingLoginChallengeId() == null || user.getPendingLoginCreatedAt() == null
            || user.getPendingLoginCreatedAt().isBefore(LocalDateTime.now().minusMinutes(LOGIN_CHALLENGE_MINUTES))) {
            return null;
        }
        return new LoginNoticeResponse(user.getPendingLoginChallengeId(), user.getPendingLoginCreatedAt());
    }

    public List<String> rolesOf(String userId) {
        return rolesOf(userId, userMapper.selectById(userId));
    }

    private List<String> rolesOf(String userId, User legacy) {
        List<String> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)).stream()
            .filter(identity -> "APPROVED".equals(identity.getStatus()))
            .map(UserRole::getRole).toList();
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

    private LoginResponse loginResponse(User user, String activeRole, String deviceId, List<String> roles) {
        String sessionToken = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionExpiresAt = now.plusNanos(jwtUtil.getExpirationMs() * 1_000_000L);
        int updated = userMapper.updateSessionToken(user.getPhone(), sessionToken, sessionExpiresAt,
            normalizeDeviceId(deviceId), now);
        if (updated != 1) throw new BusinessException("登录状态创建失败，请重试");
        user.setRole(roles.contains(activeRole) ? activeRole : "USER");
        user.setRoles(roles);
        user.setSessionToken(sessionToken);
        user.setSessionExpiresAt(sessionExpiresAt);
        user.setPassword(null);
        return new LoginResponse(user, jwtUtil.generateToken(
            user.getPhone(), user.getRole(), sessionToken), roles);
    }

    private boolean hasActiveSession(User user) {
        return user.getSessionToken() != null && !user.getSessionToken().isBlank()
            && user.getSessionExpiresAt() != null && user.getSessionExpiresAt().isAfter(LocalDateTime.now())
            && user.getSessionLastSeenAt() != null
            && user.getSessionLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(ACTIVE_SESSION_MINUTES));
    }

    /**
     * 演示入口密码仅由运行环境注入，避免将演示权限写入前端或仓库。
     */
    private void verifyDemoAccess(String password) {
        if (!demoAccountsEnabled || demoAccessPassword == null || demoAccessPassword.isBlank()) {
            throw new BusinessException(404, "演示权限未启用", "DEMO_LOGIN_DISABLED");
        }
        byte[] expected = demoAccessPassword.getBytes(StandardCharsets.UTF_8);
        byte[] provided = (password == null ? "" : password).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, provided)) {
            throw new BusinessException(403, "演示访问密码错误", "DEMO_ACCESS_DENIED");
        }
    }

    private BusinessException accountBanned(UserPenalty penalty) {
        String until = penalty.getEndTime() == null ? "永久" : penalty.getEndTime().toString().replace('T', ' ');
        return new BusinessException(403,
            "账号已被封禁，解除时间：" + until + "。原因：" + penalty.getReason(),
            "ACCOUNT_BANNED");
    }

    private boolean isSameDevice(User user, String deviceId) {
        String normalizedDeviceId = normalizeDeviceId(deviceId);
        return normalizedDeviceId != null && normalizedDeviceId.equals(user.getSessionDeviceId());
    }

    private String normalizeDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) return null;
        String normalized = deviceId.trim();
        return normalized.length() > 100 ? normalized.substring(0, 100) : normalized;
    }

    private String defaultRole(List<String> roles) {
        if (roles.contains("ADMIN")) return "ADMIN";
        if (roles.contains("MERCHANT")) return "MERCHANT";
        if (roles.contains("RIDER")) return "RIDER";
        return "USER";
    }
}
