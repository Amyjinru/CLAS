package com.clas.dto;

import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import java.time.LocalDateTime;

/** Task-pool view deliberately omits customer phone and identity data. */
public record RiderTaskResponse(Long orderId, Long merchantId, String merchantName, String merchantAddress,
    String deliveryAddress, Integer merchantDistanceMeters, Integer estimatedMinutes, Integer prepareMinutesSnapshot,
    Integer riderCommission, LocalDateTime promiseEndAt, String deliveryStatus, String recommendationReason) {
    public static RiderTaskResponse from(Orders order, Merchant merchant, int distance, String reason) {
        return new RiderTaskResponse(order.getId(), merchant.getId(), merchant.getMerchantName(), merchant.getAddress(), order.getDeliveryAddress(),
            distance, order.getEstimatedMinutes(), order.getPrepareMinutesSnapshot(), order.getRiderCommission(), order.getPromiseEndAt(), order.getDeliveryStatus(), reason);
    }
}
