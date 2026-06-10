package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.dto.ChatMessageResponse;
import com.clas.entity.ChatMessage;
import com.clas.entity.Orders;
import com.clas.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ChatService {

    private static final Set<String> CHAT_ALLOWED_STATUSES = Set.of("PAID", "ACCEPTED");
    private static final String ROLE_USER = "USER";
    private static final String ROLE_MERCHANT = "MERCHANT";

    private final ChatMessageMapper chatMessageMapper;
    private final OrderService orderService;

    public ChatService(ChatMessageMapper chatMessageMapper, OrderService orderService) {
        this.chatMessageMapper = chatMessageMapper;
        this.orderService = orderService;
    }

    public ChatMessageResponse send(Long orderId, String userId, String role, String content) {
        Orders order = orderService.requireOrder(orderId);

        if (ROLE_USER.equals(role)) {
            orderService.requireUserOrder(orderId, userId);
        } else if (ROLE_MERCHANT.equals(role)) {
            orderService.requireMerchantOrder(orderId, order.getMerchantId());
        } else {
            throw new BusinessException("无效的角色");
        }

        if (!CHAT_ALLOWED_STATUSES.contains(order.getStatus())) {
            throw new BusinessException("当前订单状态不支持聊天");
        }

        ChatMessage message = new ChatMessage();
        message.setOrderId(orderId);
        message.setMerchantId(order.getMerchantId());
        message.setUserId(order.getUserId());
        message.setSenderRole(role);
        message.setContent(content.trim());
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(message);

        return ChatMessageResponse.from(message);
    }

    public List<ChatMessageResponse> getByOrder(Long orderId, String userId, String role) {
        Orders order = orderService.requireOrder(orderId);

        if (ROLE_USER.equals(role)) {
            orderService.requireUserOrder(orderId, userId);
        } else if (ROLE_MERCHANT.equals(role)) {
            orderService.requireMerchantOrder(orderId, order.getMerchantId());
        } else {
            throw new BusinessException("无效的角色");
        }

        return chatMessageMapper.selectByOrderId(orderId).stream()
            .map(ChatMessageResponse::from)
            .toList();
    }

    public List<ChatMessageResponse> getByMerchant(Long merchantId, String userId) {
        return chatMessageMapper.selectByMerchantAndUser(merchantId, userId).stream()
            .map(ChatMessageResponse::from)
            .toList();
    }
}
