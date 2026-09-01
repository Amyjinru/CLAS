package com.clas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RiderLocationRequest(
    @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
    @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
    @DecimalMin("0") @DecimalMax("10000") Integer accuracyMeters
) {
}
