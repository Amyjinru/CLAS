package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.InternalDeliveryCommands.AbandonRequest;
import com.clas.dto.InternalDeliveryCommands.ActorRequest;
import com.clas.dto.InternalDeliveryCommands.ClaimRequest;
import com.clas.dto.InternalDeliveryCommands.LifecycleEventRequest;
import com.clas.dto.InternalDeliveryCommands.PredictedArrivalRequest;
import com.clas.dto.InternalDeliveryCommands.SequenceRequest;
import com.clas.dto.MerchantStatsDTO;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.service.InternalOrderDeliveryService;
import com.clas.service.OrderStatisticsService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/order/v1")
public class InternalOrderController {
    private final OrderStatisticsService statisticsService;
    private final InternalOrderDeliveryService deliveryService;

    public InternalOrderController(
        OrderStatisticsService statisticsService,
        InternalOrderDeliveryService deliveryService
    ) {
        this.statisticsService = statisticsService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/orders/{orderId}")
    public Result<Orders> getOrder(@PathVariable Long orderId) {
        return Result.ok(deliveryService.getOrder(orderId));
    }

    @GetMapping("/orders/{orderId}/items")
    public Result<List<OrderItem>> listItems(@PathVariable Long orderId) {
        return Result.ok(deliveryService.listItems(orderId));
    }

    @PostMapping("/deliveries/{orderId}/claim")
    public Result<Orders> claim(@PathVariable Long orderId, @RequestBody ClaimRequest request) {
        return Result.ok(deliveryService.claim(orderId, request));
    }

    @PostMapping("/deliveries/{orderId}/pickup")
    public Result<Orders> pickup(@PathVariable Long orderId, @RequestBody ActorRequest request) {
        return Result.ok(deliveryService.pickup(orderId, request == null ? null : request.riderId()));
    }

    @PostMapping("/deliveries/{orderId}/complete")
    public Result<Orders> complete(@PathVariable Long orderId, @RequestBody ActorRequest request) {
        return Result.ok(deliveryService.complete(orderId, request == null ? null : request.riderId()));
    }

    @PostMapping("/deliveries/{orderId}/abandon")
    public Result<Orders> abandon(@PathVariable Long orderId, @RequestBody AbandonRequest request) {
        return Result.ok(deliveryService.abandon(orderId, request));
    }

    @PostMapping("/deliveries/{orderId}/predicted-arrival")
    public Result<Orders> predictedArrival(
        @PathVariable Long orderId,
        @RequestBody PredictedArrivalRequest request
    ) {
        return Result.ok(deliveryService.updatePredictedArrival(orderId, request));
    }

    @PutMapping("/riders/{riderId}/delivery-sequence")
    public Result<List<Orders>> deliverySequence(
        @PathVariable String riderId,
        @RequestBody SequenceRequest request
    ) {
        return Result.ok(deliveryService.updateDeliverySequence(riderId, request));
    }

    @PostMapping("/lifecycle-events")
    public Result<Void> recordLifecycle(@RequestBody LifecycleEventRequest request) {
        deliveryService.recordLifecycle(request);
        return Result.ok();
    }

    @GetMapping("/merchants/completed-order-stats")
    public Result<Map<Long, OrderStatisticsService.CompletedOrderStats>> completedOrderStats(@RequestParam String ids) {
        List<Long> merchantIds = parseIds(ids);
        return Result.ok(statisticsService.getCompletedOrderStats(merchantIds));
    }

    @GetMapping("/users/{userId}/purchase-counts")
    public Result<Map<Long, Integer>> purchaseCounts(@PathVariable String userId) {
        return Result.ok(statisticsService.getUserPurchaseCounts(userId));
    }

    @GetMapping("/merchants/review-counts")
    public Result<Map<Long, Long>> reviewCounts(@RequestParam String ids) {
        List<Long> merchantIds = parseIds(ids);
        return Result.ok(statisticsService.getReviewCounts(merchantIds));
    }

    @GetMapping("/merchants/{merchantId}/stats")
    public Result<MerchantStatsDTO> merchantStats(@PathVariable Long merchantId) {
        return Result.ok(statisticsService.getMerchantStats(merchantId));
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(Long::valueOf)
            .collect(Collectors.toList());
    }
}
