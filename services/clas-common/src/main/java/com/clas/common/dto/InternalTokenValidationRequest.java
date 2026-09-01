package com.clas.common.dto;

import jakarta.validation.constraints.NotBlank;

/** Bearer token submitted by a trusted service for IAM-owned session validation. */
public record InternalTokenValidationRequest(@NotBlank String token) {
}
