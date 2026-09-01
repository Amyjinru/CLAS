package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.ChatMessageRequest;
import com.clas.dto.ChatMessageResponse;
import com.clas.service.RiderMerchantContactService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery")
public class RiderMerchantContactController {
    private final RiderMerchantContactService contactService;

    public RiderMerchantContactController(RiderMerchantContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/orders/{orderId}/merchant-messages")
    @RequireRole({"RIDER", "MERCHANT"})
    public Result<List<ChatMessageResponse>> messages(@PathVariable Long orderId) {
        return Result.ok(contactService.messages(orderId, UserContext.getUserId(), UserContext.getRole()));
    }

    @PostMapping("/orders/{orderId}/merchant-messages")
    @RequireRole({"RIDER", "MERCHANT"})
    public Result<ChatMessageResponse> send(@PathVariable Long orderId, @Valid @RequestBody ChatMessageRequest request) {
        return Result.ok(contactService.send(orderId, UserContext.getUserId(), UserContext.getRole(), request.content()));
    }
}
