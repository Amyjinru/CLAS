package com.clas.dto;

import com.clas.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long id,
    Long orderId,
    Long merchantId,
    String userId,
    String senderRole,
    String content,
    LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
            message.getId(),
            message.getOrderId(),
            message.getMerchantId(),
            message.getUserId(),
            message.getSenderRole(),
            message.getContent(),
            message.getCreatedAt()
        );
    }
}
