package com.clas.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class InternalDeliveryCommands {
    private InternalDeliveryCommands() {
    }

    public record ActorRequest(String riderId) {
    }

    public record ClaimRequest(String riderId, String mode) {
    }

    public record AbandonRequest(String riderId, String reason) {
    }

    public record PredictedArrivalRequest(LocalDateTime predictedArrivalAt) {
    }

    public record SequenceItem(Long orderId, Integer sequence, LocalDateTime predictedArrivalAt) {
    }

    public record SequenceRequest(List<SequenceItem> items) {
    }

    public record LifecycleEventRequest(
        Long orderId,
        String eventType,
        String fromStatus,
        String fromDeliveryStatus,
        String actorRole,
        String actorId,
        String remark
    ) {
    }
}
