package com.clas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RiderPhoneChangeAuditRequest(
    @NotNull(message = "请指定审核结果") Boolean approved,
    @Size(max = 255, message = "审核说明不能超过 255 个字符") String reason
) {
}
