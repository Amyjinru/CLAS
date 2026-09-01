package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.config.UserContext;
import com.clas.entity.Notification;
import com.clas.entity.User;
import com.clas.mapper.NotificationMapper;
import com.clas.mapper.UserMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    public NotificationService(NotificationMapper notificationMapper, UserMapper userMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
    }

    public void send(String userId, String title, String content) {
        send(new NotificationTarget(userId, title, content, null, null, null, null, null, null, null, null));
    }

    public void send(NotificationTarget target) {
        Notification notification = new Notification();
        notification.setUserId(target.userId());
        notification.setTitle(target.title());
        notification.setContent(target.content());
        notification.setReadFlag(false);
        notification.setType(target.type());
        notification.setTargetType(target.targetType());
        notification.setTargetId(target.targetId());
        notification.setReviewId(target.reviewId());
        notification.setReplyId(target.replyId());
        notification.setOrderId(target.orderId());
        notification.setMerchantId(target.merchantId());
        notification.setTargetPath(target.targetPath());
        notificationMapper.insert(notification);
    }

    public void notifyAdmins(String title, String content) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
            .eq(User::getRole, "ADMIN"));
        for (User admin : admins) {
            send(admin.getPhone(), title, content);
        }
    }

    public List<Notification> mine() {
        List<Notification> notifications = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
            .eq(Notification::getUserId, UserContext.getUserId())
            .orderByAsc(Notification::getReadFlag)
            .orderByDesc(Notification::getId));
        notifications.forEach(this::backfillLegacyTarget);
        return notifications;
    }

    public void markRead(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification != null && UserContext.getUserId().equals(notification.getUserId())) {
            notification.setReadFlag(true);
            notificationMapper.updateById(notification);
        }
    }

    @Transactional
    public void markAllRead() {
        for (Notification notification : mine()) {
            if (!Boolean.TRUE.equals(notification.getReadFlag())) {
                notification.setReadFlag(true);
                notificationMapper.updateById(notification);
            }
        }
    }

    public void deleteOne(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !UserContext.getUserId().equals(notification.getUserId())) {
            throw new com.clas.common.BusinessException("通知不存在或无权删除");
        }
        notificationMapper.deleteById(id);
    }

    public void deleteAllMine() {
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
            .eq(Notification::getUserId, UserContext.getUserId()));
    }

    private void backfillLegacyTarget(Notification notification) {
        if (hasTarget(notification)) {
            return;
        }
        Optional<LegacyNotificationTargetResolver.LegacyTarget> resolved =
            LegacyNotificationTargetResolver.resolve(notification);
        if (resolved.isEmpty()) {
            return;
        }
        LegacyNotificationTargetResolver.LegacyTarget target = resolved.get();
        notification.setType(target.type());
        notification.setTargetType(target.targetType());
        if (target.primaryId() != null) {
            notification.setTargetId(target.primaryId());
            notification.setOrderId(target.primaryId());
        }
        if (target.targetPath() != null) {
            notification.setTargetPath(target.targetPath());
        }
        notificationMapper.updateById(notification);
    }

    private boolean hasTarget(Notification notification) {
        return notification.getTargetType() != null && notification.getTargetPath() != null;
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
