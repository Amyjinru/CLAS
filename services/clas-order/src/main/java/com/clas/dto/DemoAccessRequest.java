package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

/** 演示身份入口的访问密码校验请求。 */
public record DemoAccessRequest(
    @NotBlank String password
) {
}
