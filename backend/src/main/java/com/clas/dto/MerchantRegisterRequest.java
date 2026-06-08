package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MerchantRegisterRequest(
    @NotBlank(message = "商家名称不能为空")
    String merchantName,

    String contactPhone,

    @NotBlank(message = "经营品类不能为空")
    String category,

    @NotBlank(message = "商家地址不能为空")
    String address,

    @NotBlank(message = "银行账号不能为空")
    @Pattern(regexp = "^\\d{9,25}$", message = "银行账号必须是9到25位数字")
    String bankAccount,

    @NotNull(message = "结算周期不能为空")
    Integer settlementCycle,

    // Optional fields for visitor merchant account registration.
    String accountPhone,
    String code,
    String username,
    String password,
    String confirmPassword
) {
}
