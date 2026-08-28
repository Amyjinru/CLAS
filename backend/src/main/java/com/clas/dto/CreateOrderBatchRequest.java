package com.clas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderBatchRequest(
    Long addressId,
    String deliveryAddress,
    String remark,
    @NotEmpty List<@Valid BatchOrderGroupRequest> merchantGroups,
    String deliveryContactName,
    String deliveryContactPhone,
    BigDecimal deliveryLongitude,
    BigDecimal deliveryLatitude
) {
}
