package com.clas.dto;

import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import java.util.List;

public record OrderResponse(
    Orders order,
    List<OrderItem> items,
    String customerCallUrl
) {
    public OrderResponse(Orders order, List<OrderItem> items) {
        this(order, items, null);
    }
}

