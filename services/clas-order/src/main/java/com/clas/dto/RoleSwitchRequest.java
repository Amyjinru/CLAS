package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleSwitchRequest(@NotBlank String role) {
}
