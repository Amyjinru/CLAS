package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 仅用于本地演示环境的快捷登录请求，不携带或暴露演示账号密码。
 */
public record DemoLoginRequest(
    @NotBlank String phone,
    String code,
    String deviceId,
    @NotBlank String accessPassword
) {
}
