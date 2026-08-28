package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountCancellationRequest(
    @NotBlank String currentPassword,
    @NotBlank String confirmation
) {
}
