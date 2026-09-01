package com.clas.dto;

import com.clas.entity.OrderLifecycleEvent;
import java.time.LocalDateTime;

public record OrderLifecycleEventResponse(
    Long id,
    String eventType,
    String fromStatus,
    String toStatus,
    String fromDeliveryStatus,
    String toDeliveryStatus,
    String actorRole,
    String remark,
    LocalDateTime createdAt
) {
    public static OrderLifecycleEventResponse from(OrderLifecycleEvent event) {
        return new OrderLifecycleEventResponse(
            event.getId(), event.getEventType(), event.getFromStatus(), event.getToStatus(),
            event.getFromDeliveryStatus(), event.getToDeliveryStatus(), event.getActorRole(),
            event.getRemark(), event.getCreatedAt()
        );
    }
}
