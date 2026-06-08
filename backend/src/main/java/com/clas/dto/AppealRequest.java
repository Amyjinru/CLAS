package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record AppealRequest(
    Long penaltyId,
    @NotBlank String content
) {
}
