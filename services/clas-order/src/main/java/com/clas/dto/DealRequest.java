package com.clas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DealRequest(
    @NotBlank String title,
    String description,
    @NotNull @Min(1) Integer originalPrice,
    @NotNull @Min(1) Integer dealPrice,
    @NotNull @Min(0) Integer stock,
    @NotNull @Min(1) Integer validDays,
    String status
) {
}

