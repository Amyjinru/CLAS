package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.common.LocalFileStorage;
import com.clas.entity.User;
import com.clas.mapper.UserMapper;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserAvatarUploadService {
    private static final long MAX_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final UserMapper userMapper;
    private final LocalFileStorage localFileStorage;

    public UserAvatarUploadService(UserMapper userMapper, LocalFileStorage localFileStorage) {
        this.userMapper = userMapper;
        this.localFileStorage = localFileStorage;
    }

    public User uploadAndUpdate(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("头像图片不能超过 2MB");
        }
        String original = file.getOriginalFilename() == null ? "avatar.jpg" : file.getOriginalFilename();
        String ext = original.contains(".")
            ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT)
            : ".jpg";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 JPG、PNG、GIF、WEBP 格式");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new BusinessException("请上传图片文件");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Path uploadDir = localFileStorage.resolveDirectory("avatars", userId);
        String url = localFileStorage.store(file, uploadDir, ext);
        user.setAvatar(url);
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }
}
