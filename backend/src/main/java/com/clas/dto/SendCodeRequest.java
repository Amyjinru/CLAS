package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 忘记密码 — 发送验证码请求。
 * 参照 auth-flow 技能的 sendForgotPasswordCode 流程。
 */
public record SendCodeRequest(
    @NotBlank String phone
) {
}
