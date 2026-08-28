package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.ChatMessageResponse;
import com.clas.dto.DeliveryCallSessionResponse;
import com.clas.entity.ChatConversation;
import com.clas.entity.ChatMessage;
import com.clas.entity.DeliveryCallSession;
import com.clas.entity.Orders;
import com.clas.entity.RiderAuditLog;
import com.clas.mapper.ChatConversationMapper;
import com.clas.mapper.ChatMessageMapper;
import com.clas.mapper.DeliveryCallSessionMapper;
import com.clas.mapper.UserMapper;
import com.clas.mapper.RiderAuditLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class RiderContactService {
    private static final String USER_RIDER = "USER_RIDER";
    private static final Set<String> ACTIVE = Set.of("ASSIGNED_WAITING_MEAL", "DELIVERING");
    private final OrderService orderService;
    private final ChatMessageMapper messages;
    private final ChatConversationMapper conversations;
    private final DeliveryCallSessionMapper calls;
    private final UserMapper users;
    private final RiderAuditLogMapper audits;
    private final ContentModerationService contentModerationService;
    private final PenaltyService penaltyService;

    public RiderContactService(OrderService orderService, ChatMessageMapper messages, ChatConversationMapper conversations,
                               DeliveryCallSessionMapper calls, UserMapper users, RiderAuditLogMapper audits,
                               ContentModerationService contentModerationService, PenaltyService penaltyService) {
        this.orderService = orderService;
        this.messages = messages;
        this.conversations = conversations;
        this.calls = calls;
        this.users = users;
        this.audits = audits;
        this.contentModerationService = contentModerationService;
        this.penaltyService = penaltyService;
    }

    public List<ChatMessageResponse> messages(Long orderId, String userId, String role) {
        return messages.selectUserRiderByOrderId(authorize(orderId, userId, role).getId()).stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional
    public ChatMessageResponse send(Long orderId, String userId, String role, String content) {
        penaltyService.assertCanCommunicate(userId);
        Orders order = authorize(orderId, userId, role);
        if (!ACTIVE.contains(order.getDeliveryStatus())) throw new BusinessException("订单送达后骑手聊天仅可查看历史记录");
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new BusinessException("消息内容不能为空");
        contentModerationService.assertChatTextAllowed(text);
        LocalDateTime now = LocalDateTime.now();
        ChatConversation conversation = conversations.selectOne(new LambdaQueryWrapper<ChatConversation>()
            .eq(ChatConversation::getOrderId, orderId).eq(ChatConversation::getConversationType, USER_RIDER));
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setOrderId(orderId); conversation.setConversationType(USER_RIDER); conversation.setUserId(order.getUserId());
            conversation.setPeerId(order.getRiderId()); conversation.setCreatedAt(now); conversation.setLastMessageAt(now); conversations.insert(conversation);
        } else { conversation.setLastMessageAt(now); conversations.updateById(conversation); }
        ChatMessage message = new ChatMessage();
        message.setOrderId(orderId); message.setConversationType(USER_RIDER); message.setUserId(order.getUserId());
        message.setRiderId(order.getRiderId()); message.setSenderRole(role); message.setContent(text); message.setCreatedAt(now); messages.insert(message);
        return ChatMessageResponse.from(message);
    }

    @Transactional
    public DeliveryCallSessionResponse createCall(Long orderId, String riderId) {
        Orders order = authorize(orderId, riderId, "RIDER");
        if (!ACTIVE.contains(order.getDeliveryStatus())) throw new BusinessException("仅配送中的订单可发起隐私电话");
        String phone = users.selectById(order.getUserId()).getPhone();
        DeliveryCallSession session = new DeliveryCallSession();
        session.setOrderId(orderId); session.setRiderId(riderId); session.setUserId(order.getUserId());
        session.setMaskedPhone(phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4));
        session.setStatus("ACTIVE"); session.setCreatedAt(LocalDateTime.now()); session.setExpiresAt(LocalDateTime.now().plusMinutes(10)); calls.insert(session);
        RiderAuditLog audit = new RiderAuditLog();
        audit.setRiderId(riderId); audit.setOperatorId(riderId); audit.setAction("CALL_SESSION_CREATED");
        audit.setReason("order=" + orderId + ";expires=" + session.getExpiresAt()); audit.setCreatedAt(LocalDateTime.now()); audits.insert(audit);
        return DeliveryCallSessionResponse.from(session);
    }

    private Orders authorize(Long orderId, String actorId, String role) {
        Orders order = orderService.requireOrder(orderId);
        boolean allowed = ("USER".equals(role) && Objects.equals(order.getUserId(), actorId))
            || ("RIDER".equals(role) && Objects.equals(order.getRiderId(), actorId));
        if (!allowed || order.getRiderId() == null) throw new BusinessException("无权访问该订单的骑手联系记录");
        return order;
    }
}
