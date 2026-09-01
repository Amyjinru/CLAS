package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record AddressRequest(
    @NotBlank String contactName,
    @NotBlank String phone,
    @NotBlank String address,
    BigDecimal longitude,
    BigDecimal latitude,
    Boolean isDefault
) {
}
