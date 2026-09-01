package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record BankCardRequest(
    @NotBlank(message = "开户银行不能为空") String bankName,
    @NotBlank(message = "持卡人不能为空") String cardholderName,
    @NotBlank(message = "银行卡号不能为空") String cardNo,
    String cardType,
    Boolean isDefault
) {
}
