package com.clas.dto;

import java.time.LocalDateTime;

public record BookingRequest(
    Long merchantId,
    String serviceName,
    LocalDateTime appointmentTime,
    String contactPhone,
    String note
) {
}
