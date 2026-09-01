package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.dto.OrderLifecycleEventResponse;
import com.clas.entity.OrderLifecycleEvent;
import com.clas.entity.Orders;
import com.clas.mapper.OrderLifecycleEventMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderLifecycleService {
    private final OrderLifecycleEventMapper events;

    public OrderLifecycleService(OrderLifecycleEventMapper events) {
        this.events = events;
    }

    public void record(Orders order, String eventType, String fromStatus, String fromDeliveryStatus,
                       String actorRole, String actorId, String remark) {
        OrderLifecycleEvent event = new OrderLifecycleEvent();
        event.setOrderId(order.getId());
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(order.getStatus());
        event.setFromDeliveryStatus(fromDeliveryStatus);
        event.setToDeliveryStatus(order.getDeliveryStatus());
        event.setActorRole(actorRole);
        event.setActorId(actorId);
        event.setRemark(remark);
        event.setCreatedAt(LocalDateTime.now());
        events.insert(event);
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
