package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
    @NotBlank String contactName,
    @NotBlank String phone,
    @NotBlank String address,
    Boolean isDefault
) {
}
