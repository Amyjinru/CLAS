package com.clas.dto;

import com.clas.entity.Coupon;
import java.time.LocalDateTime;

public record CouponResponse(
    Long id,
    String title,
    String description,
    String couponType,
    Integer discountAmount,
    Integer discountPercent,
    Integer minOrderAmount,
    Long merchantId,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    String status,
    boolean claimable
) {
    public static CouponResponse from(Coupon coupon, boolean claimable) {
        return new CouponResponse(
            coupon.getId(),
            coupon.getTitle(),
            coupon.getDescription(),
            coupon.getCouponType(),
            coupon.getDiscountAmount(),
            coupon.getDiscountPercent(),
            coupon.getMinOrderAmount(),
            coupon.getMerchantId(),
            coupon.getValidFrom(),
            coupon.getValidTo(),
            coupon.getStatus(),
            claimable
        );
    }
}
