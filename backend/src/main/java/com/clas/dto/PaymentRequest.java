package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
    @NotNull Long orderId,
    @NotBlank String userId,
    String payMethod
) {
}
