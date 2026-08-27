package com.clas.dto;

import java.util.List;

public record CreateOrderBatchResponse(
    List<OrderResponse> orders,
    Integer totalAmount
) {
}
