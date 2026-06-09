package com.clas.dto;

import java.util.List;

public record CartValidationResponse(
    List<CartItemResponse> items,
    int invalidCount,
    boolean multiMerchant
) {
}
