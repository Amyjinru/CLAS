package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleApplicationCreateRequest(
    @NotBlank(message = "申请说明不能为空")
    @Size(max = 255, message = "申请说明不能超过255个字符")
    String reason
) {
}
