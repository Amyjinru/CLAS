package com.clas.dto;

import java.util.List;

public record BatchPaymentResponse(
    List<PaymentResponse> payments,
    Integer totalAmount,
    String paymentStatus
) {
}
