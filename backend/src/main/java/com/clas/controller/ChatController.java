package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.ChatMessageRequest;
import com.clas.dto.ChatMessageResponse;
import com.clas.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            currentUserId(),
            currentRole(),
            request.content()
        ));
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

    private String currentUserId() {
        return UserContext.getUserId();
    }

    private String currentRole() {
        return UserContext.getRole();
    }
}
