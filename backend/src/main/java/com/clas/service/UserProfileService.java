package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.dto.ProfileUpdateRequest;
import com.clas.entity.User;
import com.clas.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    private final UserMapper userMapper;
    private final ContentModerationService contentModerationService;

    public UserProfileService(UserMapper userMapper, ContentModerationService contentModerationService) {
        this.userMapper = userMapper;
        this.contentModerationService = contentModerationService;
    }

    public User getProfile(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    public User updateProfile(String userId, ProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (nickname.isEmpty()) {
                throw new BusinessException("昵称不能为空");
            }
            if (nickname.length() > 50) {
                throw new BusinessException("昵称不能超过 50 个字符");
            }
            contentModerationService.assertTextAllowed(nickname, "昵称");
            user.setNickname(nickname);
        }
        if (request.avatar() != null) {
            contentModerationService.assertAvatarUrlAllowed(request.avatar());
            user.setAvatar(request.avatar());
        }
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

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
