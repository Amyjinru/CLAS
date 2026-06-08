package com.clas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RemoveCartRequest(
    @NotBlank String userId,
    @NotNull Long productId,
    @NotNull @Min(1) Integer quantity
) {
}
