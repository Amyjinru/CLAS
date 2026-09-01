package com.clas.service;

import com.clas.entity.User;
import com.clas.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class SessionTouchService {
    private final UserMapper userMapper;

    public SessionTouchService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public void touchActiveSession(User user) {
        if (user == null || user.getSessionToken() == null || user.getSessionToken().isBlank()) {
            return;
        }
        LocalDateTime lastSeenAt = user.getSessionLastSeenAt();
        if (lastSeenAt == null || lastSeenAt.isBefore(LocalDateTime.now().minusSeconds(30))) {
            userMapper.touchSession(user.getPhone(), user.getSessionToken(), LocalDateTime.now());
        }
    }
}
