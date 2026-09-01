package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleApplicationAuditRequest(
    @NotBlank(message = "审核结果不能为空")
    String status,

    @Size(max = 255, message = "备注不能超过255个字符")
    String remarks
) {
}
