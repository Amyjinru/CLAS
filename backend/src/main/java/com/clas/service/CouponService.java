package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.CouponResponse;
import com.clas.dto.UserCouponResponse;
import com.clas.entity.Coupon;
import com.clas.entity.UserCoupon;
import com.clas.mapper.CouponMapper;
import com.clas.mapper.UserCouponMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {
    public static final String STATUS_UNUSED = "UNUSED";
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    private static final String COUPON_ACTIVE = "ACTIVE";
    private static final String TYPE_FIXED = "FIXED";
    private static final String TYPE_PERCENT = "PERCENT";

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public CouponService(CouponMapper couponMapper, UserCouponMapper userCouponMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    public List<CouponResponse> listClaimable(String userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
            .eq(Coupon::getStatus, COUPON_ACTIVE)
            .le(Coupon::getValidFrom, now)
            .ge(Coupon::getValidTo, now)
            .orderByDesc(Coupon::getId));
        Map<Long, UserCoupon> claimed = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId))
            .stream()
            .collect(Collectors.toMap(UserCoupon::getCouponId, item -> item, (left, right) -> left));

        List<CouponResponse> responses = new ArrayList<>();
        for (Coupon coupon : coupons) {
            if (claimed.containsKey(coupon.getId())) {
                continue;
            }
            if (coupon.getTotalLimit() != null && coupon.getTotalLimit() > 0
                && coupon.getClaimedCount() != null
                && coupon.getClaimedCount() >= coupon.getTotalLimit()) {
                continue;
            }
            responses.add(CouponResponse.from(coupon, true));
        }
        return responses;
    }

    @Transactional
    public UserCouponResponse claim(String userId, Long couponId) {
        Coupon coupon = requireActiveCoupon(couponId);
        UserCoupon existing = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
            .eq(UserCoupon::getUserId, userId)
            .eq(UserCoupon::getCouponId, couponId));
        if (existing != null) {
            throw new BusinessException("您已领取过该优惠券");
        }
        boolean limitedAndFull = coupon.getTotalLimit() != null && coupon.getTotalLimit() > 0
            && coupon.getClaimedCount() != null
            && coupon.getClaimedCount() >= coupon.getTotalLimit();
        if (limitedAndFull) {
            throw new BusinessException("优惠券已被领完");
        }
        int incremented = couponMapper.incrementClaimedIfAvailable(couponId);
        if (incremented == 0) {
            throw new BusinessException("优惠券已被领完");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(STATUS_UNUSED);
        userCoupon.setClaimedAt(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);

        coupon.setClaimedCount((coupon.getClaimedCount() == null ? 0 : coupon.getClaimedCount()) + 1);
        return UserCouponResponse.from(userCoupon, coupon);
    }

    public List<UserCouponResponse> listMine(String userId) {
        refreshExpiredCoupons(userId);
        List<UserCoupon> userCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
            .eq(UserCoupon::getUserId, userId)
            .orderByDesc(UserCoupon::getId));
        return toResponses(userCoupons);
    }

    public List<UserCouponResponse> listAvailableForCheckout(String userId, Long merchantId, int subtotal) {
        refreshExpiredCoupons(userId);
        List<UserCoupon> userCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
            .eq(UserCoupon::getUserId, userId)
            .eq(UserCoupon::getStatus, STATUS_UNUSED)
            .orderByDesc(UserCoupon::getId));
        List<UserCouponResponse> responses = new ArrayList<>();
        for (UserCoupon userCoupon : userCoupons) {
            Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
            if (coupon == null || !isCouponActive(coupon)) {
                continue;
            }
            if (coupon.getMerchantId() != null && !coupon.getMerchantId().equals(merchantId)) {
                continue;
            }
            if (subtotal < safeAmount(coupon.getMinOrderAmount())) {
                continue;
            }
            responses.add(UserCouponResponse.from(userCoupon, coupon));
        }
        return responses;
    }

    public int calculateDiscount(Long userCouponId, String userId, Long merchantId, int subtotal) {
        if (userCouponId == null) {
            return 0;
        }
        UserCoupon userCoupon = requireUnusedUserCoupon(userCouponId, userId);
        Coupon coupon = requireActiveCoupon(userCoupon.getCouponId());
        if (coupon.getMerchantId() != null && !coupon.getMerchantId().equals(merchantId)) {
            throw new BusinessException("该优惠券不适用于当前商家");
        }
        if (subtotal < safeAmount(coupon.getMinOrderAmount())) {
            throw new BusinessException("未达到优惠券使用门槛");
        }
        return computeDiscount(coupon, subtotal);
    }

    public void reserveForOrder(Long userCouponId, String userId, Long orderId) {
        if (userCouponId == null) {
            return;
        }
        requireUnusedUserCoupon(userCouponId, userId);
        int rows = userCouponMapper.reserveForOrder(userCouponId, userId, orderId);
        if (rows == 0) {
            throw new BusinessException("优惠券不可用");
        }
    }

    @Transactional
    public void markUsed(Long userCouponId, Long orderId) {
        if (userCouponId == null) {
            return;
        }
        int rows = userCouponMapper.markUsedForOrder(userCouponId, orderId, LocalDateTime.now());
        if (rows == 0) {
            UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
            if (userCoupon != null && STATUS_USED.equals(userCoupon.getStatus()) && Objects.equals(userCoupon.getOrderId(), orderId)) {
                return;
            }
            throw new BusinessException("优惠券不可用");
        }
    }

    @Transactional
    public void releaseForOrder(Long userCouponId) {
        if (userCouponId == null) {
            return;
        }
        userCouponMapper.releaseReservation(userCouponId);
    }

    private List<UserCouponResponse> toResponses(List<UserCoupon> userCoupons) {
        List<UserCouponResponse> responses = new ArrayList<>();
        for (UserCoupon userCoupon : userCoupons) {
            Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
            if (coupon != null) {
                responses.add(UserCouponResponse.from(userCoupon, coupon));
            }
        }
        return responses;
    }

    private void refreshExpiredCoupons(String userId) {
        List<UserCoupon> userCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
            .eq(UserCoupon::getUserId, userId)
            .eq(UserCoupon::getStatus, STATUS_UNUSED));
        LocalDateTime now = LocalDateTime.now();
        for (UserCoupon userCoupon : userCoupons) {
            Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
            if (coupon != null && coupon.getValidTo() != null && coupon.getValidTo().isBefore(now)) {
                userCoupon.setStatus(STATUS_EXPIRED);
                userCouponMapper.updateById(userCoupon);
            }
        }
    }

    private UserCoupon requireUnusedUserCoupon(Long userCouponId, String userId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !Objects.equals(userCoupon.getUserId(), userId)) {
            throw new BusinessException("优惠券不存在或无权使用");
        }
        if (!STATUS_UNUSED.equals(userCoupon.getStatus())) {
            throw new BusinessException("优惠券不可用");
        }
        Coupon coupon = requireActiveCoupon(userCoupon.getCouponId());
        if (coupon.getValidTo() != null && coupon.getValidTo().isBefore(LocalDateTime.now())) {
            userCoupon.setStatus(STATUS_EXPIRED);
            userCouponMapper.updateById(userCoupon);
            throw new BusinessException("优惠券已过期");
        }
        return userCoupon;
    }

    private Coupon requireActiveCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !isCouponActive(coupon)) {
            throw new BusinessException("优惠券不存在或已失效");
        }
        return coupon;
    }

    private boolean isCouponActive(Coupon coupon) {
        if (!COUPON_ACTIVE.equals(coupon.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (coupon.getValidFrom() == null || !coupon.getValidFrom().isAfter(now))
            && (coupon.getValidTo() == null || !coupon.getValidTo().isBefore(now));
    }

    private int computeDiscount(Coupon coupon, int subtotal) {
        if (TYPE_PERCENT.equals(coupon.getCouponType())) {
            int percent = coupon.getDiscountPercent() == null ? 0 : coupon.getDiscountPercent();
            percent = Math.min(Math.max(percent, 1), 99);
            int discount = (int) Math.floor(subtotal * percent / 100.0);
            return Math.min(discount, subtotal);
        }
        int discount = coupon.getDiscountAmount() == null ? 0 : coupon.getDiscountAmount();
        return Math.min(Math.max(discount, 0), subtotal);
    }

    private int safeAmount(Integer amount) {
        return amount == null ? 0 : amount;
    }
}
