package com.clas.dto;

import com.clas.common.MerchantStatusEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MerchantAuditRequest(
    @NotNull(message = "审核状态不能为空")
    MerchantStatusEnum status,

    @Size(max = 255, message = "备注字数不能超过255个字符")
    String remarks
) {
}
