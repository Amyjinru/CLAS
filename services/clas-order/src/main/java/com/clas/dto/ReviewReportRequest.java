package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewReportRequest(@NotBlank String reason) {
}
