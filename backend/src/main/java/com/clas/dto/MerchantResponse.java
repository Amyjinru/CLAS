package com.clas.dto;

import com.clas.common.MerchantStatusEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MerchantResponse(
    Long id,
    String userId,
    String merchantName,
    String logo,
    String phone,
    String category,
    String address,
    BigDecimal longitude,
    BigDecimal latitude,
    Integer deliveryRadiusM,
    String businessHours,
    Integer deliveryFee,
    Integer minOrderPrice,
    Integer averagePrice,
    BigDecimal score,
    MerchantStatusEnum status,
    Boolean manualClosed,
    String bankAccount,
    String adminRemarks,
    Integer settlementCycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer distanceMeters,
    Integer routeDistanceMeters,
    Integer estimatedMinutes,
    Boolean deliveryAvailable,
    Long favoriteCount
) {
}
