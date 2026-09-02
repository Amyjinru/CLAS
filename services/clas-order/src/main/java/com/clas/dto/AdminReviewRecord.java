package com.clas.dto;

import java.time.LocalDateTime;

public record AdminReviewRecord(
    Long id,
    Long orderId,
    String userId,
    Integer score,
    String content,
    String merchantReply,
    String reportReason,
    String reportStatus,
    Long merchantId,
    LocalDateTime createdAt
) {
}
