package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(@NotBlank String reason) {
}
