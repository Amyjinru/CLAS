package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MerchantRegisterRequest(
    @NotBlank(message = "商家名称不能为空")
    String merchantName,

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    String phone,

    @NotBlank(message = "经营品类不能为空")
    String category,

    @NotBlank(message = "商家地址不能为空")
    String address,

    @NotBlank(message = "银行账号不能为空")
    @Pattern(regexp = "^\\d{9,25}$", message = "银行账号必须是9到25位数字")
    String bankAccount,

    @NotNull(message = "结算周期不能为空")
    Integer settlementCycle,

    // Optional fields for registration when visitor registers
    String username,
    String password
) {
}
