package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record MerchantRegisterRequest(
    @NotBlank(message = "商家名称不能为空")
    String merchantName,

    String contactPhone,

    @NotBlank(message = "经营品类不能为空")
    String category,

    @NotBlank(message = "商家地址不能为空")
    String address,

    BigDecimal longitude,
    BigDecimal latitude,
    Integer deliveryRadiusM,

    @Pattern(regexp = "^$|^\\d{9,25}$", message = "银行账号必须是9到25位数字")
    String bankAccount,

    Integer settlementCycle,
    Integer defaultPrepareMinutes,

    // Optional fields for visitor merchant account registration.
    String accountPhone,
    String code,
    String username,
    String password,
    String confirmPassword
) {
}
