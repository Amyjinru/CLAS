package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
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

    String bankAccount,

    String code,

    String phoneCode,

    String bankCode
) {
}
