package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String phone,
    @NotBlank String password,
    String code,
    String deviceId
) {
}
