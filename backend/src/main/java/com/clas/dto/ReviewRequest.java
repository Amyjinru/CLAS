package com.clas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
    @NotNull Long orderId,
    @NotBlank String userId,
    @NotNull @Min(1) @Max(5) Integer score,
    String content
) {
}
