package com.clas.dto;

import java.util.List;

public record OrderPreviewResponse(
    Long merchantId,
    Integer subtotal,
    Integer deliveryFee,
    Integer distanceMeters,
    Integer minOrderPrice,
    Integer minOrderGap,
    Integer couponDiscount,
    Long selectedUserCouponId,
    Integer totalPrice,
    boolean canCheckout,
    String message,
    List<UserCouponResponse> availableCoupons
) {
}
