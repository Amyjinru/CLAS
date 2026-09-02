package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.AdminReviewRecord;
import com.clas.dto.InternalDeliveryCommands.AbandonRequest;
import com.clas.dto.InternalDeliveryCommands.ActorRequest;
import com.clas.dto.InternalDeliveryCommands.ClaimRequest;
import com.clas.dto.InternalDeliveryCommands.LifecycleEventRequest;
import com.clas.dto.InternalDeliveryCommands.PredictedArrivalRequest;
import com.clas.dto.InternalDeliveryCommands.SequenceRequest;
import com.clas.dto.InternalPage;
import com.clas.dto.MerchantSalesRank;
import com.clas.dto.MerchantStatsDTO;
import com.clas.dto.OrderDashboardStats;
import com.clas.dto.OrderLifecycleEventResponse;
import com.clas.dto.OrderStatsDTO;
import com.clas.dto.ProductSalesRank;
import com.clas.dto.SalesOverviewDTO;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.service.InternalOrderDeliveryService;
import com.clas.service.InternalOrderQueryService;
import com.clas.service.OrderStatisticsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final InternalOrderQueryService queryService;

    public InternalOrderController(
        OrderStatisticsService statisticsService,
        InternalOrderDeliveryService deliveryService,
        InternalOrderQueryService queryService
    ) {
        this.statisticsService = statisticsService;
        this.deliveryService = deliveryService;
        this.queryService = queryService;
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

    @GetMapping("/stats/dashboard")
    public Result<OrderDashboardStats> dashboardStats(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(statisticsService.getDashboardStats(startDate, endDate));
    }

    @GetMapping("/stats/orders")
    public Result<OrderStatsDTO> orderStats(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(statisticsService.getOrderStats(startDate, endDate));
    }

    @GetMapping("/stats/sales")
    public Result<SalesOverviewDTO> salesOverview(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(statisticsService.getSalesOverview(startDate, endDate));
    }

    @GetMapping("/stats/merchant-sales")
    public Result<List<MerchantSalesRank>> merchantSalesRanking(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statisticsService.getMerchantSalesRanking(limit));
    }

    @GetMapping("/stats/merchant-sales/batch")
    public Result<Map<Long, MerchantSalesRank>> merchantSalesBatch(@RequestParam String ids) {
        return Result.ok(statisticsService.getMerchantSales(parseIds(ids)));
    }

    @GetMapping("/stats/product-sales")
    public Result<List<ProductSalesRank>> productSalesRanking(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statisticsService.getProductSalesRanking(limit));
    }

    @GetMapping("/admin/orders")
    public Result<InternalPage<Orders>> adminOrders(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String deliveryStatus,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String keyword
    ) {
        return Result.ok(queryService.listOrders(page, size, status, deliveryStatus, startDate, endDate, keyword));
    }

    @GetMapping("/admin/orders/export")
    public Result<List<Orders>> exportOrders(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String deliveryStatus,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String keyword
    ) {
        return Result.ok(queryService.exportOrders(status, deliveryStatus, startDate, endDate, keyword));
    }

    @GetMapping("/admin/reviews")
    public Result<InternalPage<AdminReviewRecord>> adminReviews(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String reportStatus,
        @RequestParam(required = false) String keyword
    ) {
        return Result.ok(queryService.listReviews(page, size, reportStatus, keyword));
    }

    @GetMapping("/admin/reviews/export")
    public Result<List<AdminReviewRecord>> exportReviews(
        @RequestParam(required = false) String reportStatus,
        @RequestParam(required = false) String keyword
    ) {
        return Result.ok(queryService.exportReviews(reportStatus, keyword));
    }

    @GetMapping("/deliveries/available")
    public Result<List<Orders>> availablePreparing() {
        return Result.ok(queryService.listAvailablePreparing());
    }

    @GetMapping("/deliveries/dispatch-pool")
    public Result<List<Orders>> dispatchPool() {
        return Result.ok(queryService.listAvailableDispatch());
    }

    @GetMapping("/riders/{riderId}/orders")
    public Result<List<Orders>> riderOrders(@PathVariable String riderId) {
        return Result.ok(queryService.listByRider(riderId));
    }

    @GetMapping("/riders/{riderId}/active-orders")
    public Result<List<Orders>> riderActiveOrders(
        @PathVariable String riderId,
        @RequestParam(defaultValue = "ASSIGNED_WAITING_MEAL,DELIVERING") String statuses
    ) {
        return Result.ok(queryService.listByRiderAndDeliveryStatuses(riderId, parseStatuses(statuses)));
    }

    @GetMapping("/riders/{riderId}/active-count")
    public Result<Long> riderActiveCount(
        @PathVariable String riderId,
        @RequestParam(defaultValue = "ASSIGNED_WAITING_MEAL,DELIVERING") String statuses
    ) {
        return Result.ok(queryService.countByRiderAndDeliveryStatuses(riderId, parseStatuses(statuses)));
    }

    @GetMapping("/deliveries/matured")
    public Result<List<Orders>> maturedDeliveries(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cutoff
    ) {
        return Result.ok(queryService.listMaturedDelivered(cutoff));
    }

    @GetMapping("/orders/{orderId}/lifecycle")
    public Result<List<OrderLifecycleEventResponse>> lifecycle(@PathVariable Long orderId) {
        return Result.ok(queryService.listLifecycle(orderId));
    }

    @GetMapping("/order-items")
    public Result<List<OrderItem>> listItemsByOrderIds(@RequestParam String orderIds) {
        return Result.ok(queryService.listItemsByOrderIds(parseIds(orderIds)));
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

    private List<String> parseStatuses(String statuses) {
        if (statuses == null || statuses.isBlank()) {
            return List.of();
        }
        return Arrays.stream(statuses.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }
}
