package com.clas.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    String userId,
    @NotNull Long merchantId,
    Long addressId,
    String deliveryAddress,
    String remark,
    Long userCouponId
) {
    public CreateOrderRequest(String userId, Long merchantId) {
        this(userId, merchantId, null, null, null, null);
    }

    public CreateOrderRequest(String userId, Long merchantId, Long addressId, String deliveryAddress) {
        this(userId, merchantId, addressId, deliveryAddress, null, null);
    }

    public CreateOrderRequest(String userId, Long merchantId, Long addressId, String deliveryAddress, String remark) {
        this(userId, merchantId, addressId, deliveryAddress, remark, null);
    }
}
