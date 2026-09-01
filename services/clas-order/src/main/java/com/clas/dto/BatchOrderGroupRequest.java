package com.clas.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchOrderGroupRequest(
    @NotNull Long merchantId,
    @NotEmpty List<@NotNull Long> productIds,
    Long userCouponId
) {
}
