package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.client.CatalogClient;
import com.clas.dto.ChatMessageResponse;
import com.clas.entity.ChatConversation;
import com.clas.entity.ChatMessage;
import com.clas.entity.Orders;
import com.clas.mapper.ChatConversationMapper;
import com.clas.mapper.ChatMessageMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderMerchantContactService {
    private static final String RIDER_MERCHANT = "RIDER_MERCHANT";
    private static final Set<String> ACTIVE = Set.of("ASSIGNED_WAITING_MEAL", "DELIVERING");
    private final OrderService orderService;
    private final CatalogClient catalogClient;
    private final ChatMessageMapper messages;
    private final ChatConversationMapper conversations;
    private final ContentModerationService contentModerationService;
    private final PenaltyService penaltyService;

    public RiderMerchantContactService(OrderService orderService, CatalogClient catalogClient,
                                       ChatMessageMapper messages, ChatConversationMapper conversations,
                                       ContentModerationService contentModerationService, PenaltyService penaltyService) {
        this.orderService = orderService;
        this.catalogClient = catalogClient;
        this.messages = messages;
        this.conversations = conversations;
        this.contentModerationService = contentModerationService;
        this.penaltyService = penaltyService;
    }

    public List<ChatMessageResponse> messages(Long orderId, String actorId, String role) {
        authorize(orderId, actorId, role, false);
        return messages.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getOrderId, orderId)
                .eq(ChatMessage::getConversationType, RIDER_MERCHANT)
                .orderByAsc(ChatMessage::getCreatedAt))
            .stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional
    public ChatMessageResponse send(Long orderId, String actorId, String role, String content) {
        penaltyService.assertCanCommunicate(actorId);
        Orders order = authorize(orderId, actorId, role, true);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new BusinessException("消息内容不能为空");
        contentModerationService.assertChatTextAllowed(text);
        LocalDateTime now = LocalDateTime.now();
        ensureConversation(order, now);
        ChatMessage message = new ChatMessage();
        message.setOrderId(orderId);
        message.setConversationType(RIDER_MERCHANT);
        message.setMerchantId(order.getMerchantId());
        message.setUserId(order.getUserId());
        message.setRiderId(order.getRiderId());
        message.setSenderRole(role);
        message.setContent(text);
        message.setCreatedAt(now);
        messages.insert(message);
        return ChatMessageResponse.from(message);
    }

    private Orders authorize(Long orderId, String actorId, String role, boolean sending) {
        Orders order = orderService.requireOrder(orderId);
        if (order.getRiderId() == null) throw new BusinessException("订单尚未分配骑手");
        if ("RIDER".equals(role)) {
            if (!Objects.equals(order.getRiderId(), actorId)) throw new BusinessException("无权联系该订单商家");
        } else if ("MERCHANT".equals(role)) {
            Long merchantId = catalogClient.getMerchantIdByUser(actorId);
            if (!Objects.equals(order.getMerchantId(), merchantId)) {
                throw new BusinessException("无权联系该订单骑手");
            }
        } else {
            throw new BusinessException("无效的沟通角色");
        }
        if (sending && !ACTIVE.contains(order.getDeliveryStatus())) {
            throw new BusinessException("订单送达后仅可查看骑手与商家沟通记录");
        }
        return order;
    }

    private void ensureConversation(Orders order, LocalDateTime now) {
        ChatConversation conversation = conversations.selectOne(new LambdaQueryWrapper<ChatConversation>()
            .eq(ChatConversation::getOrderId, order.getId())
            .eq(ChatConversation::getConversationType, RIDER_MERCHANT));
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setOrderId(order.getId());
            conversation.setConversationType(RIDER_MERCHANT);
            conversation.setUserId(order.getRiderId());
            conversation.setPeerId(String.valueOf(order.getMerchantId()));
            conversation.setCreatedAt(now);
        }
        conversation.setLastMessageAt(now);
        if (conversation.getId() == null) conversations.insert(conversation);
        else conversations.updateById(conversation);
    }
}
