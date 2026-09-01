package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.InternalNotificationRequest;
import com.clas.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/iam/v1/notifications")
public class InternalNotificationController {
    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public Result<Void> send(@Valid @RequestBody InternalNotificationRequest request) {
        notificationService.send(new NotificationService.NotificationTarget(
            request.userId(),
            request.title(),
            request.content(),
            request.type(),
            request.targetType(),
            request.targetId(),
            request.reviewId(),
            request.replyId(),
            request.orderId(),
            request.merchantId(),
            request.targetPath()
        ));
        return Result.ok();
    }
}
