package com.clas.dto;

import java.math.BigDecimal;

public record MerchantRatingResponse(
    Long merchantId,
    BigDecimal averageScore,
    Long reviewCount
) {
}
