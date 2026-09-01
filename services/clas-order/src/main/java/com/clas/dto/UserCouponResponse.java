package com.clas.dto;

import com.clas.entity.Coupon;
import com.clas.entity.UserCoupon;
import java.time.LocalDateTime;

public record UserCouponResponse(
    Long id,
    Long couponId,
    String title,
    String description,
    String couponType,
    Integer discountAmount,
    Integer discountPercent,
    Integer minOrderAmount,
    Long merchantId,
    String status,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    LocalDateTime claimedAt,
    LocalDateTime usedAt,
    Long orderId
) {
    public static UserCouponResponse from(UserCoupon userCoupon, Coupon coupon) {
        return new UserCouponResponse(
            userCoupon.getId(),
            coupon.getId(),
            coupon.getTitle(),
            coupon.getDescription(),
            coupon.getCouponType(),
            coupon.getDiscountAmount(),
            coupon.getDiscountPercent(),
            coupon.getMinOrderAmount(),
            coupon.getMerchantId(),
            userCoupon.getStatus(),
            coupon.getValidFrom(),
            coupon.getValidTo(),
            userCoupon.getClaimedAt(),
            userCoupon.getUsedAt(),
            userCoupon.getOrderId()
        );
    }
}
