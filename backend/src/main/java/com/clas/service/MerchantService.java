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
import com.clas.dto.MerchantRegisterRequest;
import com.clas.dto.MerchantResponse;
import com.clas.entity.Merchant;
import com.clas.entity.MerchantAuditLog;
import com.clas.entity.User;
import com.clas.entity.UserAddress;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.MerchantAuditLogMapper;
import com.clas.mapper.UserAddressMapper;
import com.clas.mapper.UserMapper;
import com.clas.config.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MerchantService {
    private final MerchantMapper merchantMapper;
    private final MerchantAuditLogMapper merchantAuditLogMapper;
    private final UserMapper userMapper;
    private final UserAddressMapper userAddressMapper;
    private final VerificationCodeStore verificationCodeStore;
    private final AmapRouteService amapRouteService;
    private final RecommendService recommendService;

    public MerchantService(
        MerchantMapper merchantMapper,
        MerchantAuditLogMapper merchantAuditLogMapper,
        UserMapper userMapper,
        UserAddressMapper userAddressMapper,
        VerificationCodeStore verificationCodeStore,
        AmapRouteService amapRouteService,
        RecommendService recommendService
    ) {
        this.merchantMapper = merchantMapper;
        this.merchantAuditLogMapper = merchantAuditLogMapper;
        this.userMapper = userMapper;
        this.userAddressMapper = userAddressMapper;
        this.verificationCodeStore = verificationCodeStore;
        this.amapRouteService = amapRouteService;
        this.recommendService = recommendService;
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
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getStatus, MerchantStatusEnum.OPEN);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Merchant::getMerchantName, keyword)
                .or()
                .like(Merchant::getAddress, keyword)
                .or()
                .like(Merchant::getCategory, keyword));
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(Merchant::getCategory, category);
        }
        boolean distanceSort = "distance".equals(sort);
        boolean recommendSort = sort == null || sort.isBlank() || "recommend".equals(sort);
        if ("price".equals(sort)) {
            wrapper.orderByAsc(Merchant::getAveragePrice);
        } else if ("latest".equals(sort)) {
            wrapper.orderByDesc(Merchant::getId);
        } else if (!distanceSort && !recommendSort) {
            wrapper.orderByDesc(Merchant::getScore);
        }
        List<Merchant> merchants = merchantMapper.selectList(wrapper);
        if (recommendSort && !distanceSort) {
            merchants = recommendService.sortByRecommend(merchants, UserContext.getUserId());
        }
        Coordinate coordinate = resolveCoordinate(latitude, longitude, addressId);
        return merchants.stream()
            .map(merchant -> convertToResponse(merchant, coordinate))
            .filter(response -> !Boolean.TRUE.equals(onlyDeliverable) || Boolean.TRUE.equals(response.deliveryAvailable()))
            .sorted(distanceSort ? Comparator
                .comparing((MerchantResponse response) -> response.distanceMeters() == null ? Integer.MAX_VALUE : response.distanceMeters())
                .thenComparing(MerchantResponse::score, Comparator.nullsLast(Comparator.reverseOrder())) : (left, right) -> 0)
            .collect(Collectors.toList());
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
        return convertToResponse(merchant);
    }

    public DeliveryEstimateResponse deliveryEstimate(Long id, BigDecimal latitude, BigDecimal longitude) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null || merchant.getStatus() != MerchantStatusEnum.OPEN) {
            throw new BusinessException("商家不存在或未营业");
        }
        if (!GeoUtils.hasCoordinate(longitude, latitude)) {
            throw new BusinessException("请选择当前位置");
        }
        Estimate estimate = estimateDelivery(merchant, new Coordinate(latitude, longitude));
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

    @Transactional
    public MerchantResponse register(MerchantRegisterRequest request, String loggedInUserId) {
        String finalUserId = loggedInUserId;
        String contactPhone = PhoneValidator.normalizeAndValidate(request.contactPhone());

        // 1. If not logged in, we must register a new user first
        if (finalUserId == null) {
            String accountPhone = PhoneValidator.normalizeAndValidate(request.accountPhone());
            PasswordValidator.validate(request.password());
            if (!request.password().equals(request.confirmPassword())) {
                throw new BusinessException("两次输入的密码不一致");
            }
            verificationCodeStore.verify(accountPhone, "register", request.code());

            if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank()) {
                throw new BusinessException("未登录用户注册商家，必须提供展示名和密码");
            }

            if (userMapper.selectById(accountPhone) != null) {
                throw new BusinessException("该手机号已被注册");
            }

            User user = new User();
            user.setPhone(accountPhone);
            user.setUsername(request.username());
            user.setPassword(request.password());
            user.setRole("MERCHANT");
            userMapper.insert(user);
            finalUserId = accountPhone;
        } else {
            // If already logged in, check if user is already a merchant or has merchant role
            User user = userMapper.selectById(finalUserId);
            if (user == null) {
                throw new BusinessException("登录用户不存在");
            }
            // Update user role to MERCHANT if it's currently USER
            if ("USER".equals(user.getRole())) {
                user.setRole("MERCHANT");
                userMapper.updateById(user);
            }
        }

        // 2. Business rules validation
        // One user -> one merchant
        Long merchantCount = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getUserId, finalUserId));
        if (merchantCount > 0) {
            throw new BusinessException("每个用户只能入驻一个商家");
        }

        // Merchant name uniqueness
        Long nameCount = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getMerchantName, request.merchantName()));
        if (nameCount > 0) {
            throw new BusinessException("商家名称已被占用");
        }

        // Phone uniqueness
        Long phoneCount = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getPhone, contactPhone));
        if (phoneCount > 0) {
            throw new BusinessException("联系电话已被其他商家占用");
        }

        // 3. Create merchant
        Merchant merchant = new Merchant();
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
        merchant.setBankAccount(request.bankAccount());
        merchant.setSettlementCycle(request.settlementCycle());
        
        merchantMapper.insert(merchant);

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
        return convertToResponse(merchant, null);
    }

    private MerchantResponse convertToResponse(Merchant merchant, Coordinate coordinate) {
        Estimate estimate = estimateDelivery(merchant, coordinate);
        return new MerchantResponse(
            merchant.getId(),
            merchant.getUserId(),
            merchant.getMerchantName(),
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
            merchant.getBankAccount(),
            merchant.getAdminRemarks(),
            merchant.getSettlementCycle(),
            merchant.getCreatedAt(),
            merchant.getUpdatedAt(),
            estimate.distanceMeters(),
            estimate.routeDistanceMeters(),
            estimate.estimatedMinutes(),
            estimate.deliveryAvailable()
        );
    }

    private Estimate estimateDelivery(Merchant merchant, Coordinate coordinate) {
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

    private record Coordinate(BigDecimal latitude, BigDecimal longitude) {
    }

    private record Estimate(
        Integer distanceMeters,
        Integer routeDistanceMeters,
        Integer estimatedMinutes,
        Boolean deliveryAvailable
    ) {
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
}
