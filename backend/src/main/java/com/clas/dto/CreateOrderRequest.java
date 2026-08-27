package com.clas.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
    String userId,
    @NotNull Long merchantId,
    Long addressId,
    String deliveryAddress,
    String remark,
    Long userCouponId,
    List<Long> productIds
) {
    public CreateOrderRequest(String userId, Long merchantId) {
        this(userId, merchantId, null, null, null, null, null);
    }

    public CreateOrderRequest(String userId, Long merchantId, Long addressId, String deliveryAddress) {
        this(userId, merchantId, addressId, deliveryAddress, null, null, null);
    }

    public CreateOrderRequest(String userId, Long merchantId, Long addressId, String deliveryAddress, String remark) {
        this(userId, merchantId, addressId, deliveryAddress, remark, null, null);
    }

    public CreateOrderRequest(String userId, Long merchantId, Long addressId, String deliveryAddress, String remark, Long userCouponId) {
        this(userId, merchantId, addressId, deliveryAddress, remark, userCouponId, null);
    }
}
