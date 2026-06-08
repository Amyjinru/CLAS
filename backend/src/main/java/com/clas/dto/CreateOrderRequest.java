package com.clas.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    String userId,
    @NotNull Long merchantId,
    String deliveryAddress
) {
    public CreateOrderRequest(String userId, Long merchantId) {
        this(userId, merchantId, null);
    }
}
