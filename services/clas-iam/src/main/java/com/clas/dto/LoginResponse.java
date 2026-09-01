package com.clas.dto;

import com.clas.entity.User;
import java.util.List;

public record LoginResponse(User user, String token, List<String> roles) {
    public LoginResponse(User user, String token) {
        this(user, token, user.getRoles());
    }
    /** 兼容旧调用（无 token），token 为 null */
    public LoginResponse(User user) {
        this(user, null, user.getRoles());
    }
}

