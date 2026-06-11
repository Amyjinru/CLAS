package com.clas.dto;

import com.clas.entity.Payment;
import java.time.LocalDateTime;

public record PaymentResponse(
    Long paymentId,
    Long orderId,
    Integer amount,
    String payMethod,
    String paymentStatus,
    String orderStatus,
    LocalDateTime createTime,
    String idempotencyKey
) {
    public static PaymentResponse from(Payment payment, String orderStatus) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getPayMethod(),
            payment.getStatus(),
            orderStatus,
            payment.getCreateTime(),
            payment.getIdempotencyKey()
        );
    }
}
