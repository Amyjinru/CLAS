package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.config.UserContext;
import com.clas.entity.Notification;
import com.clas.entity.User;
import com.clas.mapper.NotificationMapper;
import com.clas.mapper.UserMapper;
import java.util.List;
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
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(false);
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

    public void deleteOne(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !UserContext.getUserId().equals(notification.getUserId())) {
            throw new BusinessException("通知不存在或无权删除");
        }
        notificationMapper.deleteById(id);
    }

    public void deleteAllMine() {
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
            .eq(Notification::getUserId, UserContext.getUserId()));
    }
}
