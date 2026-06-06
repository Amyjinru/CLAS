package com.clas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartRequest(
    @NotNull Long userId,
    @NotNull Long productId,
    @NotNull @Min(1) Integer quantity
) {
}
