package com.clas.dto;

import com.clas.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long id,
    Long orderId,
    String conversationType,
    Long merchantId,
    String userId,
    String riderId,
    String senderRole,
    String content,
    LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
            message.getId(),
            message.getOrderId(),
            message.getConversationType(),
            message.getMerchantId(),
            message.getUserId(),
            message.getRiderId(),
            message.getSenderRole(),
            message.getContent(),
            message.getCreatedAt()
        );
    }
}
