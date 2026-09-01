package com.clas.service;

import com.clas.client.IamClient;
import com.clas.dto.InternalNotificationRequest;
import org.springframework.stereotype.Service;

@Service
public class NotificationBridge {
    private final IamClient iamClient;

    public NotificationBridge(IamClient iamClient) {
        this.iamClient = iamClient;
    }

    public void send(NotificationTarget target) {
        iamClient.sendNotification(new InternalNotificationRequest(
            target.userId(),
            target.title(),
            target.content(),
            target.type(),
            target.targetType(),
            target.targetId(),
            target.reviewId(),
            target.replyId(),
            target.orderId(),
            target.merchantId(),
            target.targetPath()
        ));
    }

    public void notifyAdmins(String title, String content) {
        // 管理员通知由 compat/admin 聚合服务补齐；迁移阶段先跳过。
    }

    public record NotificationTarget(
        String userId,
        String title,
        String content,
        String type,
        String targetType,
        Long targetId,
        Long reviewId,
        Long replyId,
        Long orderId,
        Long merchantId,
        String targetPath
    ) {
    }
}
