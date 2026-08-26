package com.clas.dto;

import com.clas.entity.RiderProfileChangeRequest;
import java.time.LocalDateTime;

public record RiderPhoneChangeResponse(
    Long id,
    String riderId,
    String currentPhone,
    String requestedPhone,
    String status,
    String reviewReason,
    LocalDateTime reviewedAt,
    LocalDateTime createdAt
) {
    public static RiderPhoneChangeResponse from(RiderProfileChangeRequest request) {
        return new RiderPhoneChangeResponse(
            request.getId(), request.getRiderId(), request.getCurrentPhone(), request.getRequestedPhone(), request.getStatus(),
            request.getReviewReason(), request.getReviewedAt(), request.getCreatedAt()
        );
    }
}
