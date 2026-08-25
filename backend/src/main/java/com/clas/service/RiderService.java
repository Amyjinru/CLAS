package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.OrderResponse;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.mapper.OrderItemMapper;
import com.clas.mapper.OrdersMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderService {
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;

    public RiderService(OrdersMapper ordersMapper, OrderItemMapper orderItemMapper) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
    }

    /** 最小用例：只展示已被商家接单、正在备餐且尚未指派的订单。 */
    public List<OrderResponse> listAvailableOrders() {
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getStatus, OrderService.STATUS_ACCEPTED)
            .eq(Orders::getDeliveryStatus, "PREPARING")
            .isNull(Orders::getRiderId)
            .orderByAsc(Orders::getAcceptedAt));
        return withItems(orders);
    }

    public List<OrderResponse> listMyOrders(String riderId) {
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getRiderId, riderId)
            .orderByDesc(Orders::getRiderAcceptedAt));
        return withItems(orders);
    }

    @Transactional
    public OrderResponse claim(Long orderId, String riderId) {
        int updated = ordersMapper.claimForRider(orderId, riderId);
        if (updated == 0) {
            if (ordersMapper.selectById(orderId) == null) {
                throw new BusinessException("订单不存在");
            }
            throw new BusinessException("订单已被其他骑手接走或状态已变化");
        }
        return withItems(ordersMapper.selectById(orderId));
    }

    // TODO(rider-next): 实现到店、取餐、送达状态机，并与用户/商家通知联动。
    // TODO(rider-next): 引入骑手在线状态、实时位置、距离排序、收益统计与调度策略。

    private List<OrderResponse> withItems(List<Orders> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Orders::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
            .stream()
            .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream()
            .map(order -> new OrderResponse(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
            .toList();
    }

    private OrderResponse withItems(Orders order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
            .eq(OrderItem::getOrderId, order.getId()));
        return new OrderResponse(order, items);
    }
}
