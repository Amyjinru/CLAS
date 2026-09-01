package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RiderInfoUpdateRequest(
    @NotBlank(message = "请选择配送工具") @Size(max = 20, message = "配送工具不能超过 20 个字符") String vehicleType,
    @NotBlank(message = "请填写服务区域") @Size(max = 100, message = "服务区域不能超过 100 个字符") String serviceArea,
    @NotBlank(message = "请填写紧急联系人") @Size(max = 50, message = "紧急联系人不能超过 50 个字符") String emergencyContactName,
    @NotBlank(message = "请填写紧急联系人电话") @Pattern(regexp = "^1[3-9]\\d{9}$", message = "紧急联系人电话格式不正确") String emergencyContactPhone
) {
}
