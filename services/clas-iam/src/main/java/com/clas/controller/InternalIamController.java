package com.clas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clas.common.BusinessException;
import com.clas.common.JwtUtil;
import com.clas.common.PasswordValidator;
import com.clas.common.PhoneValidator;
import com.clas.common.Result;
import com.clas.common.VerificationCodeStore;
import com.clas.common.dto.InternalTokenValidationRequest;
import com.clas.common.dto.InternalValidatedUser;
import com.clas.dto.AppealProcessRequest;
import com.clas.dto.InternalAddressResponse;
import com.clas.dto.InternalPage;
import com.clas.dto.InternalUserProfile;
import com.clas.dto.InternalUserSummary;
import com.clas.dto.MerchantApplicantRequest;
import com.clas.dto.PenaltyRequest;
import com.clas.dto.RoleStatusRequest;
import com.clas.entity.Appeal;
import com.clas.entity.Favorite;
import com.clas.entity.RoleApplication;
import com.clas.entity.User;
import com.clas.entity.UserAddress;
import com.clas.entity.UserPenalty;
import com.clas.mapper.FavoriteMapper;
import com.clas.mapper.RoleApplicationMapper;
import com.clas.mapper.UserAddressMapper;
import com.clas.mapper.UserMapper;
import com.clas.service.AppealService;
import com.clas.service.PenaltyService;
import com.clas.service.UserBankCardService;
import com.clas.service.UserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/iam/v1")
public class InternalIamController {
    private final UserMapper userMapper;
    private final UserAddressMapper userAddressMapper;
    private final FavoriteMapper favoriteMapper;
    private final RoleApplicationMapper roleApplicationMapper;
    private final UserService userService;
    private final PenaltyService penaltyService;
    private final AppealService appealService;
    private final UserBankCardService userBankCardService;
    private final VerificationCodeStore verificationCodeStore;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public InternalIamController(
        UserMapper userMapper,
        UserAddressMapper userAddressMapper,
        FavoriteMapper favoriteMapper,
        RoleApplicationMapper roleApplicationMapper,
        UserService userService,
        PenaltyService penaltyService,
        AppealService appealService,
        UserBankCardService userBankCardService,
        VerificationCodeStore verificationCodeStore,
        BCryptPasswordEncoder passwordEncoder,
        JwtUtil jwtUtil
    ) {
        this.userMapper = userMapper;
        this.userAddressMapper = userAddressMapper;
        this.favoriteMapper = favoriteMapper;
        this.roleApplicationMapper = roleApplicationMapper;
        this.userService = userService;
        this.penaltyService = penaltyService;
        this.appealService = appealService;
        this.userBankCardService = userBankCardService;
        this.verificationCodeStore = verificationCodeStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/users/{userId}")
    public Result<InternalUserSummary> getUser(@PathVariable String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.ok(null);
        }
        return Result.ok(new InternalUserSummary(user.getPhone(), user.getUsername(), user.getRole(), user.getEnabled()));
    }

    @GetMapping("/users/{userId}/profile")
    public Result<InternalUserProfile> getUserProfile(@PathVariable String userId) {
        return Result.ok(toProfile(userMapper.selectById(userId)));
    }

    @GetMapping("/users")
    public Result<InternalPage<InternalUserProfile>> listUsers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<User> wrapper = userQuery(role, enabled, keyword);
        wrapper.orderByAsc(User::getPhone);
        wrapper.select(User.class, info -> !"password".equals(info.getColumn()));
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<InternalUserProfile> records = result.getRecords().stream().map(this::toProfile).toList();
        return Result.ok(new InternalPage<>(records, result.getTotal(), result.getCurrent(), result.getSize()));
    }

    @GetMapping("/users/export")
    public Result<List<InternalUserProfile>> exportUsers(
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<User> wrapper = userQuery(role, enabled, keyword);
        wrapper.orderByAsc(User::getPhone);
        wrapper.select(User.class, info -> !"password".equals(info.getColumn()));
        return Result.ok(userMapper.selectList(wrapper).stream().map(this::toProfile).toList());
    }

    @GetMapping("/users/batch")
    public Result<List<InternalUserProfile>> getUsersBatch(@RequestParam("ids") String ids) {
        List<String> userIds = parseUserIds(ids);
        if (userIds.isEmpty()) {
            return Result.ok(List.of());
        }
        return Result.ok(userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getPhone, userIds))
            .stream()
            .map(this::toProfile)
            .toList());
    }

    @PostMapping("/auth/validate")
    public Result<InternalValidatedUser> validateToken(@RequestBody InternalTokenValidationRequest request) {
        if (!jwtUtil.isTokenValid(request.token())) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        String userId = jwtUtil.getPhoneFromToken(request.token());
        User user = userMapper.selectById(userId);
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(401, "账号已被禁用或不存在");
        }
        String tokenSession = jwtUtil.getSessionTokenFromToken(request.token());
        if (tokenSession == null || !tokenSession.equals(user.getSessionToken())) {
            throw new BusinessException(401, "账号已在其他设备登录，请重新登录");
        }
        String activeRole = jwtUtil.getRoleFromToken(request.token());
        List<String> roles = new ArrayList<>(userService.rolesOf(userId));
        if (user.getRole() != null && !user.getRole().isBlank() && !roles.contains(user.getRole())) {
            roles.add(user.getRole());
        }
        if (activeRole == null || !roles.contains(activeRole)) {
            throw new BusinessException(403, "当前身份尚未审核通过或已不可用");
        }
        if ("ADMIN".equals(activeRole) && !activeRole.equals(user.getRole())) {
            throw new BusinessException(403, "当前端口身份已失效，请重新登录");
        }
        userService.touchActiveSession(user);
        return Result.ok(new InternalValidatedUser(
            user.getPhone(), user.getUsername(), activeRole, roles, penaltyService.isAccountOnlyRestricted(userId)
        ));
    }

    @GetMapping("/users/{userId}/roles")
    public Result<List<String>> roles(@PathVariable String userId) {
        return Result.ok(userService.rolesOf(userId));
    }

    @PostMapping("/users/{userId}/roles/{role}")
    public Result<Void> grantRole(@PathVariable String userId, @PathVariable String role) {
        userService.grantRole(userId, role);
        return Result.ok();
    }

    @PutMapping("/users/{userId}/roles/{role}/status")
    public Result<Void> upsertRoleStatus(
        @PathVariable String userId,
        @PathVariable String role,
        @RequestBody RoleStatusRequest request
    ) {
        userService.upsertRoleStatus(userId, role, request == null ? null : request.status());
        return Result.ok();
    }

    @PostMapping("/users/merchant-applicant")
    public Result<String> ensureMerchantApplicant(@RequestBody MerchantApplicantRequest request) {
        if (request.loggedInUserId() != null && !request.loggedInUserId().isBlank()) {
            User user = userMapper.selectById(request.loggedInUserId());
            if (user == null) {
                throw new BusinessException("登录用户不存在");
            }
            if (!"USER".equals(user.getRole())) {
                throw new BusinessException("当前账号不是普通用户，不能申请商家身份");
            }
            return Result.ok(request.loggedInUserId());
        }
        String accountPhone = PhoneValidator.normalizeAndValidate(request.accountPhone());
        verificationCodeStore.verify(accountPhone, "register", request.code());
        User user = userMapper.selectById(accountPhone);
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("未登录用户入驻商家，必须提供账号密码");
        }
        if (user == null) {
            if (request.username() == null || request.username().isBlank()) {
                throw new BusinessException("新账号入驻商家，必须提供展示名");
            }
            PasswordValidator.validate(request.password());
            if (!request.password().equals(request.confirmPassword())) {
                throw new BusinessException("两次输入的密码不一致");
            }
            user = new User();
            user.setPhone(accountPhone);
            user.setUsername(request.username());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setRole("USER");
            userMapper.insert(user);
        } else {
            if (user.getEnabled() != null && !user.getEnabled()) {
                throw new BusinessException("账号已被禁用，请联系管理员");
            }
            boolean passwordMatches = user.getPassword() != null && user.getPassword().startsWith("$2")
                ? passwordEncoder.matches(request.password(), user.getPassword())
                : request.password().equals(user.getPassword());
            if (!passwordMatches) {
                throw new BusinessException("账号手机号或密码错误");
            }
            if (!"USER".equals(user.getRole())) {
                throw new BusinessException("当前账号不是普通用户，不能申请商家身份");
            }
        }
        return Result.ok(accountPhone);
    }

    @GetMapping("/addresses/{addressId}")
    public Result<InternalAddressResponse> getAddress(@PathVariable Long addressId, @RequestParam String userId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            return Result.ok(null);
        }
        return Result.ok(new InternalAddressResponse(
            address.getId(),
            address.getUserId(),
            address.getLongitude(),
            address.getLatitude(),
            address.getContactName(),
            address.getPhone(),
            address.getAddress()
        ));
    }

    @GetMapping("/merchants/{merchantId}/favorite-count")
    public Result<Long> favoriteCount(@PathVariable Long merchantId) {
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
            .eq(Favorite::getMerchantId, merchantId));
        return Result.ok(count);
    }

    @GetMapping("/users/{userId}/platform-access")
    public Result<Void> assertPlatformAccess(@PathVariable String userId) {
        penaltyService.assertCanUsePlatform(userId);
        return Result.ok();
    }

    @GetMapping("/users/{userId}/comment-access")
    public Result<Void> assertCommentAccess(@PathVariable String userId) {
        penaltyService.assertCanComment(userId);
        return Result.ok();
    }

    @GetMapping("/users/{userId}/rider-role-pending")
    public Result<Boolean> hasPendingRiderRoleApplication(@PathVariable String userId) {
        boolean pending = roleApplicationMapper.exists(new LambdaQueryWrapper<RoleApplication>()
            .eq(RoleApplication::getUserId, userId)
            .eq(RoleApplication::getTargetRole, "RIDER")
            .eq(RoleApplication::getStatus, "PENDING"));
        return Result.ok(pending);
    }

    @GetMapping("/bank-cards/{cardId}/owned")
    public Result<Boolean> bankCardOwned(@PathVariable Long cardId, @RequestParam String userId) {
        return Result.ok(userBankCardService.ownedBy(cardId, userId));
    }

    @PostMapping("/admin/penalties")
    public Result<UserPenalty> applyPenalty(@RequestBody PenaltyRequest request, @RequestParam String adminId) {
        return Result.ok(penaltyService.applyPenalty(request, adminId));
    }

    @PostMapping("/admin/penalties/{penaltyId}/revoke")
    public Result<Void> revokePenalty(@PathVariable Long penaltyId, @RequestParam String adminId) {
        penaltyService.revokePenalty(penaltyId, adminId);
        return Result.ok();
    }

    @PostMapping("/admin/users/{userId}/restore")
    public Result<InternalUserProfile> restoreAccount(@PathVariable String userId, @RequestParam String adminId) {
        penaltyService.restoreAccount(userId, adminId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return Result.ok(toProfile(user));
    }

    @GetMapping("/admin/appeals")
    public Result<List<Appeal>> listPendingAppeals() {
        return Result.ok(appealService.listPending());
    }

    @PostMapping("/admin/appeals/{appealId}/process")
    public Result<Appeal> processAppeal(@PathVariable Long appealId, @RequestBody AppealProcessRequest request) {
        if (request == null) {
            throw new BusinessException("申诉处理内容不能为空");
        }
        return Result.ok(appealService.process(appealId, request.status(), request.adminReply(), request.adminId()));
    }

    @GetMapping("/stats/public")
    public Result<Map<String, Long>> publicStats() {
        return Result.ok(Map.of("users", userMapper.selectCount(null)));
    }

    private LambdaQueryWrapper<User> userQuery(String role, Boolean enabled, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role);
        }
        if (enabled != null) {
            wrapper.eq(User::getEnabled, enabled);
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(w -> w.like(User::getPhone, normalizedKeyword)
                .or()
                .like(User::getUsername, normalizedKeyword)
                .or()
                .like(User::getNickname, normalizedKeyword));
        }
        return wrapper;
    }

    private InternalUserProfile toProfile(User user) {
        if (user == null) {
            return null;
        }
        return new InternalUserProfile(
            user.getPhone(),
            user.getUsername(),
            user.getRole(),
            user.getEnabled(),
            user.getNickname(),
            user.getAvatar()
        );
    }

    private List<String> parseUserIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }
}
