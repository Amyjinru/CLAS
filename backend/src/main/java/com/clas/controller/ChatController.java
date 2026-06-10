package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.ChatMessageRequest;
import com.clas.dto.ChatMessageResponse;
import com.clas.dto.ConversationSummary;
import com.clas.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    @RequireRole({"USER", "MERCHANT"})
    public Result<ChatMessageResponse> send(@Valid @RequestBody ChatMessageRequest request) {
        return Result.ok(chatService.send(
            request.orderId(),
            request.merchantId(),
            request.userId(),
            currentUserId(),
            currentRole(),
            request.content()
        ));
    }

    @PostMapping("/consult/{merchantId}")
    @RequireRole("USER")
    public Result<ChatMessageResponse> consult(@PathVariable Long merchantId, @RequestBody Map<String, String> body) {
        return Result.ok(chatService.consult(merchantId, currentUserId(), currentRole(), body.get("content")));
    }

    @GetMapping("/order/{orderId}")
    @RequireRole({"USER", "MERCHANT"})
    public Result<List<ChatMessageResponse>> getByOrder(@PathVariable Long orderId) {
        return Result.ok(chatService.getByOrder(orderId, currentUserId(), currentRole()));
    }

    @GetMapping("/merchant/{merchantId}")
    @RequireRole("USER")
    public Result<List<ChatMessageResponse>> getByMerchant(@PathVariable Long merchantId) {
        return Result.ok(chatService.getByMerchant(merchantId, currentUserId()));
    }

    @GetMapping("/with/{merchantId}")
    @RequireRole({"USER", "MERCHANT"})
    public Result<List<ChatMessageResponse>> getWithMerchant(
        @PathVariable Long merchantId,
        @RequestParam(required = false) String userId
    ) {
        return Result.ok(chatService.getWithMerchant(merchantId, currentUserId(), currentRole(), userId));
    }

    @GetMapping("/conversations")
    @RequireRole({"USER", "MERCHANT"})
    public Result<List<ConversationSummary>> getConversations() {
        return Result.ok(chatService.getConversations(currentUserId(), currentRole()));
    }

    @GetMapping("/admin/merchants")
    @RequireRole("ADMIN")
    public Result<List<Long>> getAdminMerchantIds() {
        return Result.ok(chatService.getAdminMerchantIds());
    }

    @GetMapping("/admin/merchant/{merchantId}/users")
    @RequireRole("ADMIN")
    public Result<List<String>> getAdminUserIds(@PathVariable Long merchantId) {
        return Result.ok(chatService.getAdminUserIdsByMerchant(merchantId));
    }

    @GetMapping("/admin/merchant/{merchantId}/user/{userId}")
    @RequireRole("ADMIN")
    public Result<List<ChatMessageResponse>> getAdminMessages(
        @PathVariable Long merchantId,
        @PathVariable String userId
    ) {
        return Result.ok(chatService.getAdminMessages(merchantId, userId));
    }

    private String currentUserId() {
        return UserContext.getUserId();
    }

    private String currentRole() {
        return UserContext.getRole();
    }
}
