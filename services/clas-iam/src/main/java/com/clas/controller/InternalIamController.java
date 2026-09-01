package com.clas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.JwtUtil;
import com.clas.common.PasswordValidator;
import com.clas.common.PhoneValidator;
import com.clas.common.Result;
import com.clas.common.VerificationCodeStore;
import com.clas.common.dto.InternalTokenValidationRequest;
import com.clas.common.dto.InternalValidatedUser;
import com.clas.dto.InternalAddressResponse;
import com.clas.dto.InternalUserSummary;
import com.clas.dto.MerchantApplicantRequest;
import com.clas.entity.Favorite;
import com.clas.entity.RoleApplication;
import com.clas.entity.User;
import com.clas.entity.UserAddress;
import com.clas.mapper.FavoriteMapper;
import com.clas.mapper.RoleApplicationMapper;
import com.clas.mapper.UserAddressMapper;
import com.clas.mapper.UserMapper;
import com.clas.service.PenaltyService;
import com.clas.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
            address.getLatitude()
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

    @GetMapping("/users/{userId}/rider-role-pending")
    public Result<Boolean> hasPendingRiderRoleApplication(@PathVariable String userId) {
        boolean pending = roleApplicationMapper.exists(new LambdaQueryWrapper<RoleApplication>()
            .eq(RoleApplication::getUserId, userId)
            .eq(RoleApplication::getTargetRole, "RIDER")
            .eq(RoleApplication::getStatus, "PENDING"));
        return Result.ok(pending);
    }

    @GetMapping("/stats/public")
    public Result<Map<String, Long>> publicStats() {
        return Result.ok(Map.of("users", userMapper.selectCount(null)));
    }
}
