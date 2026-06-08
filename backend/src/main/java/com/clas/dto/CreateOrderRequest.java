package com.clas.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    String userId,
    @NotNull Long merchantId
) {
}
