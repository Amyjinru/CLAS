package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record MerchantProfileUpdateRequest(
    @NotBlank(message = "店铺名称不能为空")
    String merchantName,

    @NotBlank(message = "店铺地址不能为空")
    String address,

    BigDecimal longitude,
    BigDecimal latitude,
    Integer deliveryRadiusM,
    String businessHours,

    @NotBlank(message = "联系电话不能为空")
    String phone,

    @NotBlank(message = "银行账号不能为空")
    @Pattern(regexp = "^\\d{9,25}$", message = "银行账号必须是 9 到 25 位数字")
    String bankAccount,

    String code
) {
}
