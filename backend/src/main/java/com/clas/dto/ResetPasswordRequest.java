package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 忘记密码 — 验证码 + 新密码重置请求。
 * 参照 auth-flow 技能的 resetForgotPassword 流程。
 */
public record ResetPasswordRequest(
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @NotBlank String phone,
    @NotBlank String code,
    @NotBlank String newPassword,
    String confirmPassword
) {
}
