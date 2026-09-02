package com.clas.service;

import com.clas.client.OrderClient;
import com.clas.dto.OrderLifecycleEventResponse;
import com.clas.entity.Orders;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderLifecycleService {
    private final OrderClient orderClient;

    public OrderLifecycleService(OrderClient orderClient) {
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
        return orderClient.listLifecycle(orderId);
    }
}
