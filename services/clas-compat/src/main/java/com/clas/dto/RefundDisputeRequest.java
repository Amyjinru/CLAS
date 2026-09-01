package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundDisputeRequest(@NotBlank @Size(max = 500) String reason) {
}
