package com.clas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Privacy-safe active-delivery tracking view; no telephone or identity fields are present. */
public record DeliveryTrackingResponse(
    Long orderId,
    String deliveryStatus,
    LocalDateTime promiseStartAt,
    LocalDateTime promiseEndAt,
    LocalDateTime predictedArrivalAt,
    Integer remainingMinutes,
    boolean routeAvailable,
    String routeSource,
    boolean liveLocationAvailable,
    boolean locationStale,
    BigDecimal riderLongitude,
    BigDecimal riderLatitude,
    LocalDateTime locationUpdatedAt
) {
}
