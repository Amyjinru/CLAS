package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RiderPhoneChangeRequest(
    @NotBlank(message = "请填写新的服务联系电话") @Pattern(regexp = "^1[3-9]\\d{9}$", message = "服务联系电话格式不正确") String phone
) {
}
