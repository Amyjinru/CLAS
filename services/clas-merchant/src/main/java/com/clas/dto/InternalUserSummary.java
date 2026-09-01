package com.clas.dto;

public record InternalUserSummary(
    String userId,
    String username,
    String role,
    Boolean enabled
) {
}
