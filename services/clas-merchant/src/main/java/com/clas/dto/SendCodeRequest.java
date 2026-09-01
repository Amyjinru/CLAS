package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 忘记密码 — 发送验证码请求。
 * 参照 auth-flow 技能的 sendForgotPasswordCode 流程。
 */
public record SendCodeRequest(
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @NotBlank String phone
) {
}
