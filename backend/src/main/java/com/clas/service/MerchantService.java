package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.GeoUtils;
import com.clas.common.MerchantStatusEnum;
import com.clas.common.PasswordValidator;
import com.clas.common.PhoneValidator;
import com.clas.common.VerificationCodeStore;
import com.clas.dto.MerchantAuditRequest;
import com.clas.dto.DeliveryEstimateResponse;
import com.clas.dto.MerchantProfileUpdateRequest;
import com.clas.dto.MerchantRegisterRequest;
import com.clas.dto.MerchantResponse;
import com.clas.dto.SendCodeRequest;
import com.clas.entity.Merchant;
import com.clas.entity.MerchantAuditLog;
import com.clas.entity.User;
import com.clas.entity.UserAddress;
import com.clas.entity.Favorite;
import com.clas.entity.Orders;
import com.clas.entity.Product;
import com.clas.entity.RoleApplication;
import com.clas.mapper.FavoriteMapper;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.MerchantAuditLogMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ProductMapper;
import com.clas.mapper.RiderApplicationMapper;
import com.clas.mapper.RoleApplicationMapper;
import com.clas.mapper.UserAddressMapper;
import com.clas.mapper.UserMapper;
import com.clas.config.UserContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MerchantService {
    private static final int COMPLETED_ORDER_THRESHOLD = 10;
    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    private static final String PRODUCT_STATUS_DELETED = "DELETED";

    private final MerchantMapper merchantMapper;
    private final MerchantAuditLogMapper merchantAuditLogMapper;
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserAddressMapper userAddressMapper;
    private final OrdersMapper ordersMapper;
    private final ProductMapper productMapper;
    private final RiderApplicationMapper riderApplicationMapper;
    private final RoleApplicationMapper roleApplicationMapper;
    private final FavoriteMapper favoriteMapper;
    private final VerificationCodeStore verificationCodeStore;
    private final AmapRouteService amapRouteService;
    private final RecommendService recommendService;
    private final BCryptPasswordEncoder passwordEncoder;

    public MerchantService(
        MerchantMapper merchantMapper,
        MerchantAuditLogMapper merchantAuditLogMapper,
        UserService userService,
        UserMapper userMapper,
        UserAddressMapper userAddressMapper,
        OrdersMapper ordersMapper,
        ProductMapper productMapper,
        RiderApplicationMapper riderApplicationMapper,
        RoleApplicationMapper roleApplicationMapper,
        FavoriteMapper favoriteMapper,
        VerificationCodeStore verificationCodeStore,
        AmapRouteService amapRouteService,
        RecommendService recommendService,
        BCryptPasswordEncoder passwordEncoder
    ) {
        this.merchantMapper = merchantMapper;
        this.merchantAuditLogMapper = merchantAuditLogMapper;
        this.userService = userService;
        this.userMapper = userMapper;
        this.userAddressMapper = userAddressMapper;
        this.ordersMapper = ordersMapper;
        this.productMapper = productMapper;
        this.riderApplicationMapper = riderApplicationMapper;
        this.roleApplicationMapper = roleApplicationMapper;
        this.favoriteMapper = favoriteMapper;
        this.verificationCodeStore = verificationCodeStore;
        this.amapRouteService = amapRouteService;
        this.recommendService = recommendService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<MerchantResponse> list() {
        return search(null, null, "recommend", null, null, null, false);
    }

    public List<MerchantResponse> search(String keyword, String category, String sort) {
        return search(keyword, category, sort, null, null, null, false);
    }

    public List<MerchantResponse> search(
        String keyword,
        String category,
        String sort,
        BigDecimal latitude,
        BigDecimal longitude,
        Long addressId,
        Boolean onlyDeliverable
    ) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        String normalizedCategory = category == null ? null : category.trim();
        String normalizedSort = normalizeSort(sort);
        boolean distanceSort = "distance".equals(normalizedSort);
        boolean recommendSort = "recommend".equals(normalizedSort);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getStatus, MerchantStatusEnum.OPEN);
        if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
            wrapper.and(w -> w.like(Merchant::getMerchantName, normalizedKeyword)
                .or()
                .like(Merchant::getAddress, normalizedKeyword)
                .or()
                .like(Merchant::getCategory, normalizedKeyword));
        }
        if (normalizedCategory != null && !normalizedCategory.isBlank()) {
            wrapper.eq(Merchant::getCategory, normalizedCategory);
        }
        if ("price".equals(normalizedSort)) {
            wrapper.orderByAsc(Merchant::getAveragePrice);
        } else if ("latest".equals(normalizedSort)) {
            wrapper.orderByDesc(Merchant::getId);
        } else if (!distanceSort && !recommendSort) {
            wrapper.orderByDesc(Merchant::getScore);
        }
        List<Merchant> merchants = merchantMapper.selectList(wrapper);
        refreshAveragePrices(merchants.stream().map(Merchant::getId).toList());
        merchants = merchantMapper.selectList(wrapper);
        if (recommendSort && !distanceSort) {
            merchants = recommendService.sortByRecommend(merchants, UserContext.getUserId());
        }
        Coordinate coordinate = resolveCoordinate(latitude, longitude, addressId);
        return merchants.stream()
            .map(merchant -> convertToResponse(merchant, coordinate, false))
            .filter(response -> !Boolean.TRUE.equals(onlyDeliverable) || Boolean.TRUE.equals(response.deliveryAvailable()))
            .sorted(distanceSort ? Comparator
                .comparing((MerchantResponse response) -> response.distanceMeters() == null ? Integer.MAX_VALUE : response.distanceMeters())
                .thenComparing(MerchantResponse::score, Comparator.nullsLast(Comparator.reverseOrder())) : (left, right) -> 0)
            .collect(Collectors.toList());
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "score";
        }
        String normalized = sort.trim();
        if ("distance".equals(normalized) || "score".equals(normalized) ||
            "price".equals(normalized) || "latest".equals(normalized) ||
            "recommend".equals(normalized)) {
            return normalized;
        }
        return "score";
    }

    public List<MerchantResponse> listAll() {
        List<Merchant> merchants = merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
            .orderByDesc(Merchant::getId));
        return merchants.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public MerchantResponse detail(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null || merchant.getStatus() != MerchantStatusEnum.OPEN) {
            throw new BusinessException("商家不存在或未营业");
        }
        refreshAveragePrice(id);
        merchant = merchantMapper.selectById(id);
        Long favoriteCount = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
            .eq(Favorite::getMerchantId, id));
        return convertToResponse(merchant, null, false, favoriteCount);
    }

    public DeliveryEstimateResponse deliveryEstimate(Long id, BigDecimal latitude, BigDecimal longitude) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null || merchant.getStatus() != MerchantStatusEnum.OPEN) {
            throw new BusinessException("商家不存在或未营业");
        }
        if (!GeoUtils.hasCoordinate(longitude, latitude)) {
            throw new BusinessException("请选择当前位置");
        }
        Estimate estimate = estimateDelivery(merchant, new Coordinate(latitude, longitude), true);
        return new DeliveryEstimateResponse(
            merchant.getId(),
            estimate.distanceMeters(),
            estimate.routeDistanceMeters(),
            estimate.estimatedMinutes(),
            merchant.getDeliveryRadiusM() == null ? 3000 : merchant.getDeliveryRadiusM(),
            estimate.deliveryAvailable()
        );
    }

    public MerchantResponse getMerchantByUserId(String userId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getUserId, userId));
        return merchant != null ? convertToResponse(merchant) : null;
    }

    public void sendPhoneUpdateCode(String phone) {
        currentMerchant();
        String nextPhone = PhoneValidator.normalizeAndValidate(phone);
        verificationCodeStore.generateAndStore(nextPhone, "merchant-phone-change");
    }

    public void sendBankUpdateCode() {
        Merchant merchant = currentMerchant();
        verificationCodeStore.generateAndStore(merchant.getPhone(), "merchant-bank-change");
    }

    public void sendRegisterCode(SendCodeRequest request) {
        String phone = PhoneValidator.normalizeAndValidate(request.phone());
        verificationCodeStore.generateAndStore(phone, "register");
    }

    public void sendProfileUpdateCode(MerchantProfileUpdateRequest request) {
        Merchant merchant = currentMerchant();
        String nextPhone = PhoneValidator.normalizeAndValidate(request.phone());
        String nextBankAccount = normalizeOptional(request.bankAccount());
        if (!nextPhone.equals(merchant.getPhone())) {
            verificationCodeStore.generateAndStore(nextPhone, "merchant-phone-change");
        }
        if (!Objects.equals(nextBankAccount, normalizeOptional(merchant.getBankAccount()))) {
            verificationCodeStore.generateAndStore(merchant.getPhone(), "merchant-bank-change");
        }
    }

    @Transactional
    public MerchantResponse updateMyProfile(MerchantProfileUpdateRequest request) {
        Merchant merchant = currentMerchant();
        String oldPhone = merchant.getPhone();
        String nextName = normalizeRequired(request.merchantName(), "店铺名称不能为空");
        String nextAddress = normalizeRequired(request.address(), "店铺地址不能为空");
        String nextPhone = PhoneValidator.normalizeAndValidate(request.phone());
        String nextBankAccount = normalizeOptional(request.bankAccount());
        if (!GeoUtils.hasCoordinate(request.longitude(), request.latitude())) {
            throw new BusinessException("请选择店铺地图位置");
        }
        boolean phoneChanged = !nextPhone.equals(oldPhone);
        boolean bankChanged = !Objects.equals(nextBankAccount, normalizeOptional(merchant.getBankAccount()));
        if (phoneChanged) {
            verificationCodeStore.verify(nextPhone, "merchant-phone-change", firstPresent(request.phoneCode(), request.code()));
        }
        if (bankChanged) {
            if (nextBankAccount.isBlank()) {
                throw new BusinessException("银行账号不能为空");
            }
            if (!nextBankAccount.matches("^\\d{9,25}$")) {
                throw new BusinessException("银行账号必须是 9 到 25 位数字");
            }
            verificationCodeStore.verify(oldPhone, "merchant-bank-change", firstPresent(request.bankCode(), request.code()));
        }
        if (phoneChanged) {
            // Check for duplicate contact phone among other merchants
            Long phoneCount = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getPhone, nextPhone)
                .ne(Merchant::getId, merchant.getId()));
            if (phoneCount > 0) {
                throw new BusinessException("联系电话已被其他商家占用");
            }
        }

        merchant.setMerchantName(nextName);
        merchant.setAddress(nextAddress);
        merchant.setLongitude(request.longitude());
        merchant.setLatitude(request.latitude());
        merchant.setDeliveryRadiusM(normalizeDeliveryRadius(request.deliveryRadiusM()));
        merchant.setBusinessHours(normalizeBusinessHours(request.businessHours(), merchant.getBusinessHours()));
        merchant.setDefaultPrepareMinutes(normalizePrepareMinutes(request.defaultPrepareMinutes(), merchant.getDefaultPrepareMinutes()));
        merchant.setPhone(nextPhone);
        merchant.setBankAccount(bankChanged ? nextBankAccount : merchant.getBankAccount());
        merchantMapper.updateById(merchant);

        return convertToResponse(merchantMapper.selectById(merchant.getId()));
    }

    @Transactional
    public MerchantResponse register(MerchantRegisterRequest request, String loggedInUserId) {
        String finalUserId = loggedInUserId;
        String contactPhone = PhoneValidator.normalizeAndValidate(request.contactPhone());

        // 1. If not logged in, we must register a new user first
        if (finalUserId == null) {
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
            finalUserId = accountPhone;
        } else {
            // 单身份模型下，只有普通用户可以提交商家申请；审核通过后才授予商家身份。
            User user = userMapper.selectById(finalUserId);
            if (user == null) {
                throw new BusinessException("登录用户不存在");
            }
            if (!"USER".equals(user.getRole())) {
                throw new BusinessException("当前账号不是普通用户，不能申请商家身份");
            }
        }

        if (userService.rolesOf(finalUserId).stream().anyMatch(role -> "MERCHANT".equals(role) || "RIDER".equals(role))) {
            throw new BusinessException("已拥有商家或骑手身份，不能申请其他业务身份");
        }
        boolean riderPending = riderApplicationMapper.exists(new LambdaQueryWrapper<com.clas.entity.RiderApplication>()
            .eq(com.clas.entity.RiderApplication::getUserId, finalUserId)
            .eq(com.clas.entity.RiderApplication::getStatus, "PENDING"));
        boolean legacyRiderPending = roleApplicationMapper.exists(new LambdaQueryWrapper<RoleApplication>()
            .eq(RoleApplication::getUserId, finalUserId)
            .eq(RoleApplication::getTargetRole, "RIDER")
            .eq(RoleApplication::getStatus, "PENDING"));
        if (riderPending || legacyRiderPending) {
            throw new BusinessException("已有待审核的骑手申请，暂不能申请商家身份");
        }

        // 2. Business rules validation
        // 已注销的商家档案可作为新的申请重新提交，其他状态仍保持一人一店限制。
        Merchant previousMerchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getUserId, finalUserId));
        if (previousMerchant != null && previousMerchant.getStatus() != MerchantStatusEnum.DISABLED) {
            throw new BusinessException("每个用户只能入驻一个商家");
        }

        // Merchant name uniqueness
        LambdaQueryWrapper<Merchant> nameQuery = new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getMerchantName, request.merchantName());
        if (previousMerchant != null) nameQuery.ne(Merchant::getId, previousMerchant.getId());
        Long nameCount = merchantMapper.selectCount(nameQuery);
        if (nameCount > 0) {
            throw new BusinessException("商家名称已被占用");
        }

        // Phone uniqueness
        LambdaQueryWrapper<Merchant> phoneQuery = new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getPhone, contactPhone);
        if (previousMerchant != null) phoneQuery.ne(Merchant::getId, previousMerchant.getId());
        Long phoneCount = merchantMapper.selectCount(phoneQuery);
        if (phoneCount > 0) {
            throw new BusinessException("联系电话已被其他商家占用");
        }

        // 3. Create merchant or reopen a cancelled application for review.
        Merchant merchant = previousMerchant != null ? previousMerchant : new Merchant();
        merchant.setUserId(finalUserId);
        merchant.setMerchantName(request.merchantName());
        merchant.setPhone(contactPhone);
        merchant.setCategory(request.category());
        merchant.setAddress(request.address());
        if (!GeoUtils.hasCoordinate(request.longitude(), request.latitude())) {
            throw new BusinessException("请选择商家地图位置");
        }
        merchant.setLongitude(request.longitude());
        merchant.setLatitude(request.latitude());
        merchant.setDeliveryRadiusM(normalizeDeliveryRadius(request.deliveryRadiusM()));
        merchant.setBusinessHours("09:00-21:00");
        merchant.setDeliveryFee(0);
        merchant.setMinOrderPrice(0);
        merchant.setAveragePrice(0);
        merchant.setScore(BigDecimal.valueOf(0.00));
        merchant.setStatus(MerchantStatusEnum.PENDING); // Default pending
        merchant.setManualClosed(false);
        merchant.setBankAccount(normalizeOptional(request.bankAccount()));
        merchant.setSettlementCycle(request.settlementCycle());
        merchant.setDefaultPrepareMinutes(normalizePrepareMinutes(request.defaultPrepareMinutes(), 15));
        if (previousMerchant == null) {
            merchantMapper.insert(merchant);
        } else {
            merchant.setAdminRemarks(null);
            merchantMapper.updateById(merchant);
        }

        return convertToResponse(merchant);
    }

    @Transactional
    public MerchantResponse audit(Long merchantId, MerchantAuditRequest auditRequest, String adminId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }

        MerchantStatusEnum oldStatus = merchant.getStatus();
        MerchantStatusEnum newStatus = auditRequest.status();

        // Validate state machine flow
        validateStatusTransition(oldStatus, newStatus);

        if (newStatus == MerchantStatusEnum.APPROVED) {
            User applicant = userMapper.selectById(merchant.getUserId());
            if (applicant == null) {
                throw new BusinessException("商家申请账号不存在");
            }
            userService.grantRole(applicant.getPhone(), "MERCHANT");
        }

        // Update status and remarks
        merchant.setStatus(newStatus);
        if (auditRequest.remarks() != null) {
            merchant.setAdminRemarks(auditRequest.remarks());
        }
        merchantMapper.updateById(merchant);

        // Log audit operation
        MerchantAuditLog log = new MerchantAuditLog();
        log.setMerchantId(merchantId);
        log.setAdminId(adminId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setRemarks(auditRequest.remarks());
        merchantAuditLogMapper.insert(log);

        return convertToResponse(merchant);
    }

    public List<MerchantAuditLog> getAuditLogs(Long merchantId) {
        return merchantAuditLogMapper.selectList(new LambdaQueryWrapper<MerchantAuditLog>()
            .eq(MerchantAuditLog::getMerchantId, merchantId)
            .orderByDesc(MerchantAuditLog::getId));
    }

    private void validateStatusTransition(MerchantStatusEnum current, MerchantStatusEnum target) {
        if (current == target) {
            return;
        }
        if (current == MerchantStatusEnum.PENDING) {
            if (target != MerchantStatusEnum.APPROVED && target != MerchantStatusEnum.BLOCKED) {
                throw new BusinessException("待审核商家只能更新为已审核或禁用状态");
            }
        } else if (current == MerchantStatusEnum.APPROVED) {
            if (target != MerchantStatusEnum.OPEN && target != MerchantStatusEnum.CLOSED && target != MerchantStatusEnum.BLOCKED) {
                throw new BusinessException("已审核商家只能更新为营业、停业或禁用状态");
            }
        } else if (current == MerchantStatusEnum.OPEN) {
            if (target != MerchantStatusEnum.CLOSED && target != MerchantStatusEnum.BLOCKED) {
                throw new BusinessException("营业中商家只能更新为停业或禁用状态");
            }
        } else if (current == MerchantStatusEnum.CLOSED) {
            if (target != MerchantStatusEnum.OPEN && target != MerchantStatusEnum.BLOCKED) {
                throw new BusinessException("停业商家只能更新为营业或禁用状态");
            }
        } else if (current == MerchantStatusEnum.BLOCKED) {
            if (target != MerchantStatusEnum.APPROVED && target != MerchantStatusEnum.CLOSED && target != MerchantStatusEnum.OPEN) {
                throw new BusinessException("禁用商家只能更新为已审核、停业或营业状态");
            }
        }
    }

    private MerchantResponse convertToResponse(Merchant merchant) {
        return convertToResponse(merchant, null, false, null);
    }

    private MerchantResponse convertToResponse(Merchant merchant, Coordinate coordinate) {
        return convertToResponse(merchant, coordinate, true, null);
    }

    private MerchantResponse convertToResponse(Merchant merchant, Coordinate coordinate, boolean includeRouteEstimate) {
        return convertToResponse(merchant, coordinate, includeRouteEstimate, null);
    }

    private MerchantResponse convertToResponse(
        Merchant merchant,
        Coordinate coordinate,
        boolean includeRouteEstimate,
        Long favoriteCount
    ) {
        Estimate estimate = estimateDelivery(merchant, coordinate, includeRouteEstimate);
        return new MerchantResponse(
            merchant.getId(),
            merchant.getUserId(),
            merchant.getMerchantName(),
            merchant.getLogo(),
            merchant.getPhone(),
            merchant.getCategory(),
            merchant.getAddress(),
            merchant.getLongitude(),
            merchant.getLatitude(),
            merchant.getDeliveryRadiusM(),
            merchant.getBusinessHours(),
            merchant.getDeliveryFee(),
            merchant.getMinOrderPrice(),
            merchant.getAveragePrice(),
            merchant.getScore(),
            merchant.getStatus(),
            Boolean.TRUE.equals(merchant.getManualClosed()),
            merchant.getBankAccount(),
            merchant.getAdminRemarks(),
            merchant.getSettlementCycle(),
            merchant.getDefaultPrepareMinutes(),
            merchant.getCreatedAt(),
            merchant.getUpdatedAt(),
            estimate.distanceMeters(),
            estimate.routeDistanceMeters(),
            estimate.estimatedMinutes(),
            estimate.deliveryAvailable(),
            favoriteCount
        );
    }

    private Estimate estimateDelivery(Merchant merchant, Coordinate coordinate, boolean includeRouteEstimate) {
        if (coordinate == null || !GeoUtils.hasCoordinate(merchant.getLongitude(), merchant.getLatitude())) {
            return new Estimate(null, null, null, null);
        }
        int distanceMeters = GeoUtils.distanceMeters(
            coordinate.latitude(),
            coordinate.longitude(),
            merchant.getLatitude(),
            merchant.getLongitude()
        );
        int radius = merchant.getDeliveryRadiusM() == null ? 3000 : merchant.getDeliveryRadiusM();
        boolean deliveryAvailable = distanceMeters <= radius;
        if (!includeRouteEstimate) {
            return new Estimate(distanceMeters, null, estimateMinutes(distanceMeters), deliveryAvailable);
        }
        Optional<AmapRouteService.RouteEstimate> route = amapRouteService.estimateDriving(
            merchant.getLongitude(),
            merchant.getLatitude(),
            coordinate.longitude(),
            coordinate.latitude()
        );
        Integer routeDistanceMeters = route.map(AmapRouteService.RouteEstimate::distanceMeters).orElse(null);
        int estimatedMinutes = route
            .map(AmapRouteService.RouteEstimate::durationMinutes)
            .map(minutes -> Math.max(20, minutes + 10))
            .orElseGet(() -> estimateMinutes(distanceMeters));
        return new Estimate(distanceMeters, routeDistanceMeters, estimatedMinutes, deliveryAvailable);
    }

    private int estimateMinutes(int distanceMeters) {
        return Math.max(20, 20 + (int) Math.ceil(distanceMeters / 500.0) * 5);
    }

    private Coordinate resolveCoordinate(BigDecimal latitude, BigDecimal longitude, Long addressId) {
        if (addressId != null) {
            UserAddress address = userAddressMapper.selectById(addressId);
            if (address == null || !UserContext.getUserId().equals(address.getUserId())) {
                throw new BusinessException("地址不存在或无权操作");
            }
            if (!GeoUtils.hasCoordinate(address.getLongitude(), address.getLatitude())) {
                throw new BusinessException("该地址缺少地图坐标");
            }
            return new Coordinate(address.getLatitude(), address.getLongitude());
        }
        if (GeoUtils.hasCoordinate(longitude, latitude)) {
            return new Coordinate(latitude, longitude);
        }
        return null;
    }

    private int normalizeDeliveryRadius(Integer radius) {
        if (radius == null) {
            return 3000;
        }
        if (radius < 500 || radius > 10000) {
            throw new BusinessException("配送范围需在500到10000米之间");
        }
        return radius;
    }

    private int normalizePrepareMinutes(Integer minutes, Integer currentValue) {
        int value = minutes == null ? (currentValue == null ? 15 : currentValue) : minutes;
        if (value < 1 || value > 20) {
            throw new BusinessException("默认备餐时长需在 1 到 20 分钟之间");
        }
        return value;
    }

    @Transactional
    public MerchantResponse toggleManualClosed() {
        Merchant merchant = currentMerchant();
        if (merchant.getStatus() != MerchantStatusEnum.OPEN) {
            throw new BusinessException("只有营业中的商家可以手动打烊");
        }
        merchant.setManualClosed(!Boolean.TRUE.equals(merchant.getManualClosed()));
        merchantMapper.updateById(merchant);
        return convertToResponse(merchantMapper.selectById(merchant.getId()));
    }

    private String normalizeBusinessHours(String value, String currentValue) {
        String hours = value == null ? "" : value.trim();
        if (hours.isEmpty()) {
            return currentValue == null || currentValue.isBlank() ? "09:00-21:00" : currentValue.trim();
        }
        String[] parts = hours.split("-");
        if (parts.length != 2) {
            throw new BusinessException("营业时间格式应为 HH:mm-HH:mm");
        }
        try {
            LocalTime.parse(parts[0].trim());
            LocalTime.parse(parts[1].trim());
        } catch (DateTimeParseException exception) {
            throw new BusinessException("营业时间格式应为 HH:mm-HH:mm");
        }
        return parts[0].trim() + "-" + parts[1].trim();
    }

    private record Coordinate(BigDecimal latitude, BigDecimal longitude) {
    }

    private record Estimate(
        Integer distanceMeters,
        Integer routeDistanceMeters,
        Integer estimatedMinutes,
        Boolean deliveryAvailable
    ) {
    }

    private Merchant currentMerchant() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getUserId, userId));
        if (merchant == null) {
            throw new BusinessException("当前用户未入驻为商家");
        }
        return merchant;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstPresent(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    public Long getCurrentMerchantId() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getUserId, userId));
        if (merchant == null) {
            throw new BusinessException("当前用户未入驻为商家");
        }
        return merchant.getId();
    }

    public void refreshAveragePrice(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            return;
        }
        int averagePrice = computeAveragePrice(merchantId);
        if (!Objects.equals(merchant.getAveragePrice(), averagePrice)) {
            merchant.setAveragePrice(averagePrice);
            merchantMapper.updateById(merchant);
        }
    }

    public void refreshAveragePrices(Collection<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return;
        }
        List<Long> ids = merchantIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, CompletedOrderStats> completedStats = loadCompletedOrderStats(ids);
        Map<Long, Integer> productAverages = loadProductAveragePrices(ids);
        for (Long merchantId : ids) {
            Merchant merchant = merchantMapper.selectById(merchantId);
            if (merchant == null) {
                continue;
            }
            CompletedOrderStats stats = completedStats.getOrDefault(merchantId, CompletedOrderStats.EMPTY);
            int averagePrice = stats.count() < COMPLETED_ORDER_THRESHOLD
                ? productAverages.getOrDefault(merchantId, 0)
                : stats.averagePrice();
            if (!Objects.equals(merchant.getAveragePrice(), averagePrice)) {
                merchant.setAveragePrice(averagePrice);
                merchantMapper.updateById(merchant);
            }
        }
    }

    private int computeAveragePrice(Long merchantId) {
        CompletedOrderStats stats = loadCompletedOrderStats(List.of(merchantId))
            .getOrDefault(merchantId, CompletedOrderStats.EMPTY);
        if (stats.count() < COMPLETED_ORDER_THRESHOLD) {
            return loadProductAveragePrices(List.of(merchantId)).getOrDefault(merchantId, 0);
        }
        return stats.averagePrice();
    }

    private Map<Long, CompletedOrderStats> loadCompletedOrderStats(Collection<Long> merchantIds) {
        List<Orders> completedOrders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .in(Orders::getMerchantId, merchantIds)
            .eq(Orders::getStatus, ORDER_STATUS_COMPLETED));
        Map<Long, CompletedOrderStats> stats = new HashMap<>();
        for (Orders order : completedOrders) {
            CompletedOrderStats current = stats.getOrDefault(order.getMerchantId(), CompletedOrderStats.EMPTY);
            stats.put(order.getMerchantId(), current.add(order.getTotalPrice()));
        }
        return stats;
    }

    private Map<Long, Integer> loadProductAveragePrices(Collection<Long> merchantIds) {
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
            .in(Product::getMerchantId, merchantIds)
            .ne(Product::getStatus, PRODUCT_STATUS_DELETED));
        Map<Long, List<Integer>> pricesByMerchant = new HashMap<>();
        for (Product product : products) {
            if (product.getPrice() == null) {
                continue;
            }
            pricesByMerchant
                .computeIfAbsent(product.getMerchantId(), key -> new java.util.ArrayList<>())
                .add(product.getPrice());
        }
        Map<Long, Integer> averages = new HashMap<>();
        for (Map.Entry<Long, List<Integer>> entry : pricesByMerchant.entrySet()) {
            averages.put(entry.getKey(), averageInt(entry.getValue()));
        }
        return averages;
    }

    private int averageInt(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (Integer value : values) {
            total += value == null ? 0 : value;
        }
        return (int) Math.round((double) total / values.size());
    }

    private record CompletedOrderStats(long count, long totalPrice) {
        private static final CompletedOrderStats EMPTY = new CompletedOrderStats(0, 0);

        private CompletedOrderStats add(Integer totalPrice) {
            int price = totalPrice == null ? 0 : totalPrice;
            return new CompletedOrderStats(count + 1, this.totalPrice + price);
        }

        private int averagePrice() {
            if (count <= 0) {
                return 0;
            }
            return (int) Math.round((double) totalPrice / count);
        }
    }
}
