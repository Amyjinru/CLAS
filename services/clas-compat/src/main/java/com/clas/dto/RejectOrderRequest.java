package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectOrderRequest(
    @NotBlank String reason
) {
}
