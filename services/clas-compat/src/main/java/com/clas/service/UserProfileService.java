package com.clas.service;

import com.clas.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    public String displayName(User user) {
        if (user == null) {
            return "匿名用户";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getPhone();
    }

    public String avatarOf(User user) {
        if (user == null || user.getAvatar() == null || user.getAvatar().isBlank()) {
            return null;
        }
        return user.getAvatar();
    }
}
