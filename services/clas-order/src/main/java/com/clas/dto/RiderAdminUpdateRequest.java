package com.clas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RiderAdminUpdateRequest(@NotNull Boolean enabled, @Min(1) @Max(10) Integer maxActiveOrders, @NotBlank String reason) {
}
