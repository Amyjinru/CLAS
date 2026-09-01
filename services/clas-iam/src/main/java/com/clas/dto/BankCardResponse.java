package com.clas.dto;

import java.time.LocalDateTime;

public record BankCardResponse(
    Long id,
    String bankName,
    String cardholderName,
    String maskedCardNo,
    String cardLast4,
    String cardType,
    Boolean isDefault,
    LocalDateTime createTime
) {
}
