package com.clas.dto;

import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import java.util.List;

public record OrderResponse(
    Orders order,
    List<OrderItem> items,
    String customerCallUrl,
    String merchantName,
    String merchantLogo,
    List<OrderProductSummary> products
) {
    public OrderResponse(Orders order, List<OrderItem> items) {
        this(order, items, null, null, null, List.of());
    }

    public OrderResponse(Orders order, List<OrderItem> items, String customerCallUrl) {
        this(order, items, customerCallUrl, null, null, List.of());
    }
}
