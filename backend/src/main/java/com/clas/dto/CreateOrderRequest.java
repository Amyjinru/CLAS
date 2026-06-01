package com.clas.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    @NotNull Long userId,
    @NotNull Long merchantId
) {
}

