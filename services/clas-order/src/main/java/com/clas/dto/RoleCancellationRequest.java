package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleCancellationRequest(@NotBlank String role) {
}
