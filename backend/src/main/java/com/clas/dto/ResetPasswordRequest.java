package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 忘记密码 — 验证码 + 新密码重置请求。
 * 参照 auth-flow 技能的 resetForgotPassword 流程。
 */
public record ResetPasswordRequest(
    @NotBlank String phone,
    @NotBlank String code,
    @NotBlank String newPassword
) {
}
