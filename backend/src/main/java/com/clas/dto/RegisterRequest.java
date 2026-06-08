package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 注册请求 — 手机号 + 验证码必填，验证通过后方可注册。
 */
public record RegisterRequest(
    @NotBlank String username,
    @NotBlank String password,
    String confirmPassword,
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @NotBlank String phone,
    @NotBlank String code,
    String role
) {
}
