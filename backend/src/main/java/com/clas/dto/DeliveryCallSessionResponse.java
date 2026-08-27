package com.clas.dto;

import com.clas.entity.DeliveryCallSession;
import java.time.LocalDateTime;

public record DeliveryCallSessionResponse(Long id, Long orderId, String maskedPhone, LocalDateTime expiresAt) {
    public static DeliveryCallSessionResponse from(DeliveryCallSession session) {
        return new DeliveryCallSessionResponse(session.getId(), session.getOrderId(), session.getMaskedPhone(), session.getExpiresAt());
    }
}
