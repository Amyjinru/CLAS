package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.OrderClient;
import com.clas.dto.OrderLifecycleEventResponse;
import com.clas.entity.OrderLifecycleEvent;
import com.clas.entity.Orders;
import com.clas.mapper.OrderLifecycleEventMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderLifecycleService {
    private final OrderLifecycleEventMapper events;
    private final OrderClient orderClient;

    public OrderLifecycleService(OrderLifecycleEventMapper events, OrderClient orderClient) {
        this.events = events;
        this.orderClient = orderClient;
    }

    public void record(Orders order, String eventType, String fromStatus, String fromDeliveryStatus,
                       String actorRole, String actorId, String remark) {
        orderClient.recordLifecycle(
            order.getId(),
            eventType,
            fromStatus,
            fromDeliveryStatus,
            actorRole,
            actorId,
            remark
        );
    }

    public List<OrderLifecycleEventResponse> list(Long orderId) {
        return events.selectList(new LambdaQueryWrapper<OrderLifecycleEvent>()
                .eq(OrderLifecycleEvent::getOrderId, orderId)
                .orderByAsc(OrderLifecycleEvent::getCreatedAt)
                .orderByAsc(OrderLifecycleEvent::getId))
            .stream()
            .map(OrderLifecycleEventResponse::from)
            .toList();
    }
}
