package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record InternalNotificationRequest(
    @NotBlank String userId,
    @NotBlank String title,
    @NotBlank String content,
    String type,
    String targetType,
    Long targetId,
    Long reviewId,
    Long replyId,
    Long orderId,
    Long merchantId,
    String targetPath
) {
}
