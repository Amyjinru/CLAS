package com.clas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RemoveCartRequest(
    String userId,
    @NotNull Long productId,
    @NotNull @Min(1) Integer quantity
) {
}
