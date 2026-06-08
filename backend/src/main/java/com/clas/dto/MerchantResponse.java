package com.clas.dto;

import com.clas.common.MerchantStatusEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MerchantResponse(
    Long id,
    String userId,
    String merchantName,
    String phone,
    String category,
    String address,
    String businessHours,
    Integer deliveryFee,
    Integer minOrderPrice,
    Integer averagePrice,
    BigDecimal score,
    MerchantStatusEnum status,
    String bankAccount,
    String adminRemarks,
    Integer settlementCycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
