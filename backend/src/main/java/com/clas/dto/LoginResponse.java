package com.clas.dto;

import com.clas.entity.User;

public record LoginResponse(User user, String token) {
    /** 兼容旧调用（无 token），token 为 null */
    public LoginResponse(User user) {
        this(user, null);
    }
}

