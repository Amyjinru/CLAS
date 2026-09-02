package com.clas.dto;

import java.math.BigDecimal;

public record InternalAddressResponse(
    Long id,
    String userId,
    BigDecimal longitude,
    BigDecimal latitude,
    String contactName,
    String phone,
    String address
) {
}
