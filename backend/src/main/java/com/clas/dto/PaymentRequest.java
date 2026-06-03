package com.clas.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
    @NotNull Long orderId,
    @NotNull Long userId,
    String payMethod
) {
}
