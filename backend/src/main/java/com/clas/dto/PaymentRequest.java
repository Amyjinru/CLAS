package com.clas.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
    @NotNull Long orderId,
    String userId,
    String payMethod,
    String idempotencyKey
) {
    public PaymentRequest(Long orderId, String userId, String payMethod) {
        this(orderId, userId, payMethod, null);
    }
}
