package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.LoginRequest;
import com.clas.dto.LoginResponse;
import com.clas.dto.RegisterRequest;
import com.clas.entity.User;
import com.clas.mapper.UserMapper;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    // 第一版只允许这三种演示角色，避免注册接口写入不一致的角色字符串。
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "MERCHANT", "ADMIN");

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, request.username()));
        if (user == null || !request.password().equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        user.setPassword(null);
        return new LoginResponse(user);
    }

    public User register(RegisterRequest request) {
        // 空角色统一落到普通用户，保持注册入口的默认身份稳定。
        String role = request.role() == null || request.role().isBlank() ? "USER" : request.role().trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new BusinessException("角色只能是 USER、MERCHANT 或 ADMIN");
        }

        // 空手机号按未填写处理，避免唯一索引里出现无意义的空字符串。
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();

        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, request.username()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 手机号是登录身份资料的一部分，注册时提前拦截重复值。
        if (phone != null) {
            Long phoneCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
            if (phoneCount > 0) {
                throw new BusinessException("手机号已存在");
            }
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setPhone(phone);
        user.setRole(role);
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }
}
