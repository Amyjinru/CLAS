package com.clas.service;

import com.clas.client.MerchantClient;
import com.clas.common.BusinessException;
import com.clas.dto.ChatMessageResponse;
import com.clas.dto.ConversationSummary;
import com.clas.entity.ChatMessage;
import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import com.clas.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ChatService {

    private static final Set<String> CHAT_ALLOWED_STATUSES = Set.of("PAID", "ACCEPTED");
    private static final String ROLE_USER = "USER";
    private static final String ROLE_MERCHANT = "MERCHANT";

    private final ChatMessageMapper chatMessageMapper;
    private final OrderService orderService;
    private final MerchantClient merchantClient;
    private final ContentModerationService contentModerationService;
    private final PenaltyService penaltyService;

    public ChatService(
        ChatMessageMapper chatMessageMapper,
        OrderService orderService,
        MerchantClient merchantClient,
        ContentModerationService contentModerationService,
        PenaltyService penaltyService
    ) {
        this.chatMessageMapper = chatMessageMapper;
        this.orderService = orderService;
        this.merchantClient = merchantClient;
        this.contentModerationService = contentModerationService;
        this.penaltyService = penaltyService;
    }

    public ChatMessageResponse send(Long orderId, Long merchantId, String targetUserId, String userId, String role, String content) {
        penaltyService.assertCanCommunicate(userId);
        if (orderId == null) {
            return sendDirect(merchantId, targetUserId, userId, role, content);
        }
        Orders order = orderService.requireOrder(orderId);

        if (ROLE_USER.equals(role)) {
            orderService.requireUserOrder(orderId, userId);
        } else if (ROLE_MERCHANT.equals(role)) {
            Merchant merchant = requireCurrentMerchant(userId);
            orderService.requireMerchantOrder(orderId, merchant.getId());
        } else {
            throw new BusinessException("无效的角色");
        }

        if (!CHAT_ALLOWED_STATUSES.contains(order.getStatus())) {
            throw new BusinessException("当前订单状态不支持聊天");
        }

        String text = requireAllowedChatText(content);
        ChatMessage message = new ChatMessage();
        message.setOrderId(orderId);
        message.setMerchantId(order.getMerchantId());
        message.setUserId(order.getUserId());
        message.setSenderRole(role);
        message.setContent(text);
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(message);

        return ChatMessageResponse.from(message);
    }

    public ChatMessageResponse consult(Long merchantId, String userId, String role, String content) {
        if (!ROLE_USER.equals(role)) {
            throw new BusinessException("仅用户可发起咨询");
        }
        penaltyService.assertCanCommunicate(userId);
        return sendDirect(merchantId, userId, userId, role, content);
    }

    public List<ChatMessageResponse> getByOrder(Long orderId, String userId, String role) {
        Orders order = orderService.requireOrder(orderId);

        if (ROLE_USER.equals(role)) {
            orderService.requireUserOrder(orderId, userId);
        } else if (ROLE_MERCHANT.equals(role)) {
            Merchant merchant = requireCurrentMerchant(userId);
            orderService.requireMerchantOrder(orderId, merchant.getId());
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

    public List<ChatMessageResponse> getWithMerchant(Long merchantId, String userId, String role, String targetUserId) {
        String conversationUserId = userId;
        if (ROLE_MERCHANT.equals(role)) {
            Merchant merchant = requireCurrentMerchant(userId);
            if (!Objects.equals(merchant.getId(), merchantId)) {
                throw new BusinessException("只能查看自己店铺的对话");
            }
            if (targetUserId == null || targetUserId.isBlank()) {
                throw new BusinessException("请选择要查看的用户");
            }
            conversationUserId = targetUserId;
        } else if (!ROLE_USER.equals(role)) {
            throw new BusinessException("无效的角色");
        }
        return chatMessageMapper.selectByMerchantAndUser(merchantId, conversationUserId).stream()
            .map(ChatMessageResponse::from)
            .toList();
    }

    public List<ConversationSummary> getConversations(String userId, String role) {
        List<ChatMessage> messages;
        if (ROLE_USER.equals(role)) {
            messages = chatMessageMapper.selectLatestByUserGroupedByMerchant(userId);
        } else if (ROLE_MERCHANT.equals(role)) {
            Merchant merchant = findCurrentMerchant(userId);
            if (merchant == null) {
                return List.of();
            }
            messages = chatMessageMapper.selectLatestByMerchantGroupedByUser(merchant.getId());
        } else {
            throw new BusinessException("无效的角色");
        }
        return messages.stream().map(ConversationSummary::from).toList();
    }

    public List<Long> getAdminMerchantIds() {
        return chatMessageMapper.selectDistinctMerchantIds();
    }

    public List<String> getAdminUserIdsByMerchant(Long merchantId) {
        return chatMessageMapper.selectDistinctUserIdsByMerchant(merchantId);
    }

    public List<ChatMessageResponse> getAdminMessages(Long merchantId, String userId) {
        return chatMessageMapper.selectByMerchantAndUser(merchantId, userId).stream()
            .map(ChatMessageResponse::from)
            .toList();
    }

    private ChatMessageResponse sendDirect(Long merchantId, String targetUserId, String currentUserId, String role, String content) {
        if (merchantId == null) {
            throw new BusinessException("请选择商家");
        }
        Merchant merchant = merchantClient.getMerchant(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }

        String conversationUserId;
        if (ROLE_USER.equals(role)) {
            conversationUserId = currentUserId;
        } else if (ROLE_MERCHANT.equals(role)) {
            Merchant currentMerchant = requireCurrentMerchant(currentUserId);
            if (!Objects.equals(currentMerchant.getId(), merchantId)) {
                throw new BusinessException("只能回复自己店铺的客户");
            }
            if (targetUserId == null || targetUserId.isBlank()) {
                throw new BusinessException("请选择要回复的用户");
            }
            conversationUserId = targetUserId;
        } else {
            throw new BusinessException("无效的角色");
        }

        String text = requireAllowedChatText(content);

        ChatMessage message = new ChatMessage();
        message.setOrderId(null);
        message.setMerchantId(merchantId);
        message.setUserId(conversationUserId);
        message.setSenderRole(role);
        message.setContent(text);
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(message);
        return ChatMessageResponse.from(message);
    }

    private Merchant requireCurrentMerchant(String userId) {
        Merchant merchant = findCurrentMerchant(userId);
        if (merchant == null) {
            throw new BusinessException("当前用户不是商家");
        }
        return merchant;
    }

    private String requireAllowedChatText(String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        contentModerationService.assertChatTextAllowed(text);
        return text;
    }

    private Merchant findCurrentMerchant(String userId) {
        Long merchantId = merchantClient.getMerchantIdByUser(userId);
        return merchantId == null ? null : merchantClient.getMerchant(merchantId);
    }
}
