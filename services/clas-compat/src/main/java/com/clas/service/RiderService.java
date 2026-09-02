package com.clas.service;

import com.clas.client.OrderClient;
import com.clas.dto.OrderResponse;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderService {
    private final OrderClient orderClient;

    public RiderService(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    /** 最小用例：只展示已被商家接单、正在备餐且尚未指派的订单。 */
    public List<OrderResponse> listAvailableOrders() {
        return withItems(orderClient.listAvailablePreparing());
    }

    public List<OrderResponse> listMyOrders(String riderId) {
        return withItems(orderClient.listRiderOrders(riderId));
    }

    @Transactional
    public OrderResponse claim(Long orderId, String riderId) {
        return withItems(orderClient.claim(orderId, riderId, "PREPARING"));
    }

    private List<OrderResponse> withItems(List<Orders> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Orders::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderClient.listItemsByOrderIds(orderIds).stream()
            .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream()
            .map(order -> new OrderResponse(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
            .toList();
    }

    private OrderResponse withItems(Orders order) {
        return new OrderResponse(order, orderClient.listItems(order.getId()));
    }
}
