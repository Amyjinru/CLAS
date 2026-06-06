package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 注册请求 — 手机号 + 验证码必填，验证通过后方可注册。
 */
public record RegisterRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String phone,
    @NotBlank String code,
    String role
) {
}
