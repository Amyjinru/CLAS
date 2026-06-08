package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PenaltyRequest(
    @NotBlank String userId,
    @NotBlank String penaltyType,
    @NotBlank String reason,
    Integer durationHours
) {
}
