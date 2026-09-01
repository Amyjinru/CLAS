package com.clas.dto;

public record DeliveryEstimateResponse(
    Long merchantId,
    Integer distanceMeters,
    Integer routeDistanceMeters,
    Integer estimatedMinutes,
    Integer deliveryRadiusM,
    Boolean deliveryAvailable
) {
}
