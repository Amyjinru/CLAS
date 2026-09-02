package com.clas.dto;

public record InternalUserProfile(
    String phone,
    String username,
    String role,
    Boolean enabled,
    String nickname,
    String avatar
) {
}
