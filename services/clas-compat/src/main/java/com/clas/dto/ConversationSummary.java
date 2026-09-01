package com.clas.dto;

import com.clas.entity.ChatMessage;
import java.time.LocalDateTime;

public record ConversationSummary(
    Long merchantId,
    String userId,
    String lastMessage,
    String lastSenderRole,
    LocalDateTime lastMessageTime,
    Long lastOrderId
) {
    public static ConversationSummary from(ChatMessage message) {
        return new ConversationSummary(
            message.getMerchantId(),
            message.getUserId(),
            message.getContent(),
            message.getSenderRole(),
            message.getCreatedAt(),
            message.getOrderId()
        );
    }
}
