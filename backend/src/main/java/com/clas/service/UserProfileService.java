package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clas.common.BusinessException;
import com.clas.common.JwtUtil;
import com.clas.common.PasswordValidator;
import com.clas.common.PhoneValidator;
import com.clas.common.VerificationCodeStore;
import com.clas.dto.LoginResponse;
import com.clas.dto.PasswordChangeRequest;
import com.clas.dto.PhoneChangeRequest;
import com.clas.dto.ProfileUpdateRequest;
import com.clas.dto.SendCodeRequest;
import com.clas.entity.User;
import com.clas.mapper.UserMapper;
import java.util.List;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {
    private static final String PHONE_CHANGE_SCENE = "phone-change";
    private static final List<UserIdReference> USER_ID_REFERENCES = List.of(
        new UserIdReference("merchant", "user_id"),
        new UserIdReference("merchant_audit_log", "operator_id"),
        new UserIdReference("cart", "user_id"),
        new UserIdReference("user_address", "user_id"),
        new UserIdReference("favorite", "user_id"),
        new UserIdReference("notification", "user_id"),
        new UserIdReference("orders", "user_id"),
        new UserIdReference("chat_message", "user_id"),
        new UserIdReference("review", "user_id"),
        new UserIdReference("review_reply", "user_id"),
        new UserIdReference("review_vote", "user_id"),
        new UserIdReference("review_user_hidden", "user_id"),
        new UserIdReference("deleted_review_backup", "user_id"),
        new UserIdReference("review_delete_request", "reporter_user_id"),
        new UserIdReference("payment", "user_id"),
        new UserIdReference("service_booking", "user_id"),
        new UserIdReference("deal_order", "user_id"),
        new UserIdReference("deal_redeem_log", "operator_id"),
        new UserIdReference("user_coupon", "user_id"),
        new UserIdReference("user_penalty", "user_id"),
        new UserIdReference("appeal", "user_id"),
        new UserIdReference("user_bank_card", "user_id")
    );

    private final UserMapper userMapper;
    private final ContentModerationService contentModerationService;
    private final VerificationCodeStore verificationCodeStore;
    private final JwtUtil jwtUtil;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserProfileService(
        UserMapper userMapper,
        ContentModerationService contentModerationService,
        VerificationCodeStore verificationCodeStore,
        JwtUtil jwtUtil,
        JdbcTemplate jdbcTemplate,
        BCryptPasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.contentModerationService = contentModerationService;
        this.verificationCodeStore = verificationCodeStore;
        this.jwtUtil = jwtUtil;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public User getProfile(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    public void sendPhoneChangeCode(String userId, SendCodeRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new BusinessException("账号已被禁用，无法修改绑定手机号");
        }
        String newPhone = validateNewPhone(userId, request.phone());
        verificationCodeStore.generateAndStore(newPhone, PHONE_CHANGE_SCENE);
    }

    @Transactional
    public LoginResponse changePhone(String userId, PhoneChangeRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new BusinessException("账号已被禁用，无法修改绑定手机号");
        }
        String newPhone = validateNewPhone(userId, request.phone());
        verificationCodeStore.verify(newPhone, PHONE_CHANGE_SCENE, request.code());

        int updated = userMapper.update(null, new LambdaUpdateWrapper<User>()
            .eq(User::getPhone, userId)
            .set(User::getPhone, newPhone));
        if (updated != 1) {
            throw new BusinessException("绑定手机号修改失败，请刷新后重试");
        }
        for (UserIdReference reference : USER_ID_REFERENCES) {
            updateUserReference(reference, newPhone, userId);
        }

        user.setPhone(newPhone);
        user.setPassword(null);
        String token = jwtUtil.generateToken(newPhone, user.getRole());
        return new LoginResponse(user, token);
    }

    private void updateUserReference(UserIdReference reference, String newPhone, String oldPhone) {
        try {
            jdbcTemplate.update(
                "UPDATE " + reference.table() + " SET " + reference.column() + " = ? WHERE " + reference.column() + " = ?",
                newPhone,
                oldPhone
            );
        } catch (BadSqlGrammarException ignored) {
            // Older local/test schemas may not have every historical audit table column yet.
        }
    }

    private String validateNewPhone(String currentPhone, String phone) {
        String newPhone = PhoneValidator.normalizeAndValidate(phone);
        if (newPhone.equals(currentPhone)) {
            throw new BusinessException("新手机号不能与当前手机号相同");
        }
        if (userMapper.selectById(newPhone) != null) {
            throw new BusinessException("该手机号已被注册");
        }
        return newPhone;
    }

    public User updateProfile(String userId, ProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (nickname.isEmpty()) {
                throw new BusinessException("昵称不能为空");
            }
            if (nickname.length() > 50) {
                throw new BusinessException("昵称不能超过 50 个字符");
            }
            contentModerationService.assertTextAllowed(nickname, "昵称");
            user.setNickname(nickname);
        }
        if (request.avatar() != null) {
            contentModerationService.assertAvatarUrlAllowed(request.avatar());
            user.setAvatar(request.avatar());
        }
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    public void changePassword(String userId, PasswordChangeRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new BusinessException("账号已被禁用，无法修改密码");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        PasswordValidator.validate(request.newPassword());
        String storedPassword = user.getPassword();
        boolean currentMatches = storedPassword != null && storedPassword.startsWith("$2")
            ? passwordEncoder.matches(request.currentPassword(), storedPassword)
            : request.currentPassword().equals(storedPassword);
        if (!currentMatches) {
            throw new BusinessException("当前密码错误");
        }
        boolean samePassword = storedPassword != null && storedPassword.startsWith("$2")
            ? passwordEncoder.matches(request.newPassword(), storedPassword)
            : request.newPassword().equals(storedPassword);
        if (samePassword) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    public String displayName(User user) {
        if (user == null) {
            return "匿名用户";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getPhone();
    }

    public String avatarOf(User user) {
        if (user == null || user.getAvatar() == null || user.getAvatar().isBlank()) {
            return null;
        }
        return user.getAvatar();
    }

    private record UserIdReference(String table, String column) {
    }
}
