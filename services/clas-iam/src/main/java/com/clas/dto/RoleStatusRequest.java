package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleStatusRequest(
    @NotBlank String status
) {
}
