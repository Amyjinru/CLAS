package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchRoleRequest(@NotBlank String role) {
}
