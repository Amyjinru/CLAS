package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.config.UserContext;
import com.clas.entity.Notification;
import com.clas.mapper.NotificationMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public void send(String userId, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(false);
        notificationMapper.insert(notification);
    }

    public List<Notification> mine() {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
            .eq(Notification::getUserId, UserContext.getUserId())
            .orderByAsc(Notification::getReadFlag)
            .orderByDesc(Notification::getId));
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
}
