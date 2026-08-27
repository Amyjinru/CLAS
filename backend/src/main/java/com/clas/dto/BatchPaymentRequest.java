package com.clas.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchPaymentRequest(
    @NotEmpty List<@NotNull Long> orderIds,
    String payMethod,
    String idempotencyKey
) {
}
