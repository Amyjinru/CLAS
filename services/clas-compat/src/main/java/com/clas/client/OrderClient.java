package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.dto.AdminReviewRecord;
import com.clas.dto.InternalPage;
import com.clas.dto.MerchantSalesRank;
import com.clas.dto.MerchantStatsDTO;
import com.clas.dto.OrderDashboardStats;
import com.clas.dto.OrderLifecycleEventResponse;
import com.clas.dto.OrderStatsDTO;
import com.clas.dto.ProductSalesRank;
import com.clas.dto.SalesOverviewDTO;
import com.clas.entity.OrderItem;
import com.clas.entity.OrderRefundDispute;
import com.clas.entity.Orders;
import com.clas.entity.Review;
import com.clas.entity.ReviewDeleteRequest;
import com.clas.entity.DeletedReviewBackup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OrderClient {
    private final RestTemplate restTemplate;
    private final ServiceEndpoints serviceEndpoints;
    private final ObjectMapper objectMapper;

    public OrderClient(RestTemplate restTemplate, ServiceEndpoints serviceEndpoints, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.serviceEndpoints = serviceEndpoints;
        this.objectMapper = objectMapper;
    }

    public Map<Long, CompletedOrderStats> getCompletedOrderStats(Collection<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        String ids = merchantIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        Map<Long, CompletedOrderStats> stats = get(
            "/merchants/completed-order-stats?ids=" + ids,
            new ParameterizedTypeReference<Result<Map<Long, CompletedOrderStats>>>() {}
        );
        return stats == null ? Map.of() : stats;
    }

    public Map<Long, Integer> getUserPurchaseCounts(String userId) {
        if (userId == null || userId.isBlank()) {
            return Map.of();
        }
        Map<Long, Integer> counts = get(
            "/users/" + userId + "/purchase-counts",
            new ParameterizedTypeReference<Result<Map<Long, Integer>>>() {}
        );
        return counts == null ? Map.of() : counts;
    }

    public Map<Long, Long> getReviewCounts(List<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        String ids = merchantIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        Map<Long, Long> counts = get(
            "/merchants/review-counts?ids=" + ids,
            new ParameterizedTypeReference<Result<Map<Long, Long>>>() {}
        );
        if (counts == null) {
            Map<Long, Long> empty = new HashMap<>();
            for (Long merchantId : merchantIds) {
                empty.put(merchantId, 0L);
            }
            return empty;
        }
        return counts;
    }

    public MerchantStatsDTO getMerchantStats(Long merchantId) {
        MerchantStatsDTO stats = get(
            "/merchants/" + merchantId + "/stats",
            new ParameterizedTypeReference<Result<MerchantStatsDTO>>() {}
        );
        return stats == null
            ? new MerchantStatsDTO(0L, 0L, 0L, 0L, 0L, List.of(), List.of())
            : stats;
    }

    public Orders getOrder(Long orderId) {
        return get("/orders/" + orderId, new ParameterizedTypeReference<Result<Orders>>() {});
    }

    public List<OrderItem> listItems(Long orderId) {
        List<OrderItem> items = get(
            "/orders/" + orderId + "/items",
            new ParameterizedTypeReference<Result<List<OrderItem>>>() {}
        );
        return items == null ? List.of() : items;
    }

    public List<OrderItem> listItemsByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        String ids = orderIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        List<OrderItem> items = get(
            "/order-items?orderIds=" + ids,
            new ParameterizedTypeReference<Result<List<OrderItem>>>() {}
        );
        return items == null ? List.of() : items;
    }

    public List<Orders> listAvailablePreparing() {
        List<Orders> orders = get("/deliveries/available", new ParameterizedTypeReference<Result<List<Orders>>>() {});
        return orders == null ? List.of() : orders;
    }

    public List<Orders> listDispatchPool() {
        List<Orders> orders = get("/deliveries/dispatch-pool", new ParameterizedTypeReference<Result<List<Orders>>>() {});
        return orders == null ? List.of() : orders;
    }

    public List<Orders> listRiderOrders(String riderId) {
        List<Orders> orders = get(
            "/riders/" + riderId + "/orders",
            new ParameterizedTypeReference<Result<List<Orders>>>() {}
        );
        return orders == null ? List.of() : orders;
    }

    public List<Orders> listActiveOrders(String riderId) {
        return listActiveOrders(riderId, "ASSIGNED_WAITING_MEAL,DELIVERING");
    }

    public List<Orders> listActiveOrders(String riderId, String statuses) {
        List<Orders> orders = get(
            "/riders/" + riderId + "/active-orders?statuses=" + statuses,
            new ParameterizedTypeReference<Result<List<Orders>>>() {}
        );
        return orders == null ? List.of() : orders;
    }

    public long countActiveOrders(String riderId, String statuses) {
        Long count = get(
            "/riders/" + riderId + "/active-count?statuses=" + statuses,
            new ParameterizedTypeReference<Result<Long>>() {}
        );
        return count == null ? 0L : count;
    }

    public List<Orders> listMaturedDeliveries(LocalDateTime cutoff) {
        List<Orders> orders = get(
            "/deliveries/matured?cutoff=" + cutoff,
            new ParameterizedTypeReference<Result<List<Orders>>>() {}
        );
        return orders == null ? List.of() : orders;
    }

    public List<OrderLifecycleEventResponse> listLifecycle(Long orderId) {
        List<OrderLifecycleEventResponse> events = get(
            "/orders/" + orderId + "/lifecycle",
            new ParameterizedTypeReference<Result<List<OrderLifecycleEventResponse>>>() {}
        );
        return events == null ? List.of() : events;
    }

    public OrderDashboardStats getDashboardStats(LocalDate startDate, LocalDate endDate) {
        OrderDashboardStats stats = get(
            statsPath("/stats/dashboard", startDate, endDate),
            new ParameterizedTypeReference<Result<OrderDashboardStats>>() {}
        );
        return stats == null ? new OrderDashboardStats(0L, 0L, 0L, 0L, 0L, 0L, 0L) : stats;
    }

    public OrderStatsDTO getOrderStats(LocalDate startDate, LocalDate endDate) {
        OrderStatsDTO stats = get(
            statsPath("/stats/orders", startDate, endDate),
            new ParameterizedTypeReference<Result<OrderStatsDTO>>() {}
        );
        return stats == null ? new OrderStatsDTO(List.of(), List.of()) : stats;
    }

    public SalesOverviewDTO getSalesOverview(LocalDate startDate, LocalDate endDate) {
        SalesOverviewDTO stats = get(
            statsPath("/stats/sales", startDate, endDate),
            new ParameterizedTypeReference<Result<SalesOverviewDTO>>() {}
        );
        return stats == null ? new SalesOverviewDTO(List.of(), 0L, 0L, 0L) : stats;
    }

    public List<MerchantSalesRank> getMerchantSalesRanking() {
        List<MerchantSalesRank> ranks = get(
            "/stats/merchant-sales?limit=10",
            new ParameterizedTypeReference<Result<List<MerchantSalesRank>>>() {}
        );
        return ranks == null ? List.of() : ranks;
    }

    public Map<Long, MerchantSalesRank> getMerchantSales(Collection<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        String ids = merchantIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        Map<Long, MerchantSalesRank> ranks = get(
            "/stats/merchant-sales/batch?ids=" + ids,
            new ParameterizedTypeReference<Result<Map<Long, MerchantSalesRank>>>() {}
        );
        return ranks == null ? Map.of() : ranks;
    }

    public List<ProductSalesRank> getProductSalesRanking() {
        List<ProductSalesRank> ranks = get(
            "/stats/product-sales?limit=10",
            new ParameterizedTypeReference<Result<List<ProductSalesRank>>>() {}
        );
        return ranks == null ? List.of() : ranks;
    }

    public InternalPage<Orders> listAdminOrders(
        int page,
        int size,
        String status,
        String deliveryStatus,
        LocalDate startDate,
        LocalDate endDate,
        String keyword
    ) {
        InternalPage<Orders> result = get(
            adminOrderPath("/admin/orders", page, size, status, deliveryStatus, startDate, endDate, keyword),
            new ParameterizedTypeReference<Result<InternalPage<Orders>>>() {}
        );
        return result == null ? new InternalPage<>(List.of(), 0, page, size) : result;
    }

    public List<Orders> exportOrders(
        String status,
        String deliveryStatus,
        LocalDate startDate,
        LocalDate endDate,
        String keyword
    ) {
        List<Orders> orders = get(
            adminOrderPath("/admin/orders/export", null, null, status, deliveryStatus, startDate, endDate, keyword),
            new ParameterizedTypeReference<Result<List<Orders>>>() {}
        );
        return orders == null ? List.of() : orders;
    }

    public InternalPage<AdminReviewRecord> listAdminReviews(int page, int size, String reportStatus, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromPath("/admin/reviews")
            .queryParam("page", page)
            .queryParam("size", size);
        if (reportStatus != null && !reportStatus.isBlank()) {
            builder.queryParam("reportStatus", reportStatus);
        }
        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("keyword", keyword);
        }
        InternalPage<AdminReviewRecord> result = get(
            builder.build().toUriString(),
            new ParameterizedTypeReference<Result<InternalPage<AdminReviewRecord>>>() {}
        );
        return result == null ? new InternalPage<>(List.of(), 0, page, size) : result;
    }

    public List<AdminReviewRecord> exportReviews(String reportStatus, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/reviews/export");
        if (reportStatus != null && !reportStatus.isBlank()) {
            builder.queryParam("reportStatus", reportStatus);
        }
        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("keyword", keyword);
        }
        List<AdminReviewRecord> reviews = get(
            builder.build().toUriString(),
            new ParameterizedTypeReference<Result<List<AdminReviewRecord>>>() {}
        );
        return reviews == null ? List.of() : reviews;
    }

    public List<ReviewDeleteRequest> listDeleteRequests(String status) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/reviews/delete-requests");
        if (status != null && !status.isBlank()) {
            builder.queryParam("status", status);
        }
        List<ReviewDeleteRequest> requests = get(
            builder.build().toUriString(),
            new ParameterizedTypeReference<Result<List<ReviewDeleteRequest>>>() {}
        );
        return requests == null ? List.of() : requests;
    }

    public List<DeletedReviewBackup> listDeletedBackups() {
        List<DeletedReviewBackup> backups = get(
            "/admin/reviews/deleted-backups",
            new ParameterizedTypeReference<Result<List<DeletedReviewBackup>>>() {}
        );
        return backups == null ? List.of() : backups;
    }

    public void deleteReview(Long reviewId) {
        exchange(
            "/admin/reviews/" + reviewId + "/delete",
            HttpMethod.POST,
            new HttpEntity<>(Map.of()),
            new ParameterizedTypeReference<Result<Void>>() {}
        );
    }

    public Review resolveReviewReport(Long reviewId, String status) {
        return exchange(
            "/admin/reviews/" + reviewId + "/report-status",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("status", status == null ? "" : status)),
            new ParameterizedTypeReference<Result<Review>>() {}
        );
    }

    public void processDeleteRequest(Long requestId, boolean approve, String remarks, String adminId) {
        Map<String, Object> body = new HashMap<>();
        body.put("approve", approve);
        body.put("remarks", remarks == null ? "" : remarks);
        body.put("adminId", adminId == null ? "" : adminId);
        exchange(
            "/admin/reviews/delete-requests/" + requestId + "/process",
            HttpMethod.POST,
            new HttpEntity<>(body),
            new ParameterizedTypeReference<Result<Void>>() {}
        );
    }

    public List<OrderRefundDispute> listRefundDisputes(String status) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/order-refund-disputes");
        if (status != null && !status.isBlank()) {
            builder.queryParam("status", status);
        }
        List<OrderRefundDispute> disputes = get(
            builder.build().toUriString(),
            new ParameterizedTypeReference<Result<List<OrderRefundDispute>>>() {}
        );
        return disputes == null ? List.of() : disputes;
    }

    public OrderRefundDispute auditRefundDispute(Long disputeId, boolean approved, String reason, String adminId) {
        Map<String, Object> body = new HashMap<>();
        body.put("approved", approved);
        body.put("reason", reason == null ? "" : reason);
        body.put("adminId", adminId == null ? "" : adminId);
        return exchange(
            "/admin/order-refund-disputes/" + disputeId + "/audit",
            HttpMethod.POST,
            new HttpEntity<>(body),
            new ParameterizedTypeReference<Result<OrderRefundDispute>>() {}
        );
    }

    public Orders claim(Long orderId, String riderId, String mode) {
        return exchange(
            "/deliveries/" + orderId + "/claim",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("riderId", riderId, "mode", mode)),
            new ParameterizedTypeReference<Result<Orders>>() {}
        );
    }

    public Orders pickup(Long orderId, String riderId) {
        return actorCommand(orderId, "pickup", riderId);
    }

    public Orders complete(Long orderId, String riderId) {
        return actorCommand(orderId, "complete", riderId);
    }

    public Orders abandon(Long orderId, String riderId, String reason) {
        Map<String, String> body = new HashMap<>();
        body.put("riderId", riderId);
        body.put("reason", reason == null ? "" : reason);
        return exchange(
            "/deliveries/" + orderId + "/abandon",
            HttpMethod.POST,
            new HttpEntity<>(body),
            new ParameterizedTypeReference<Result<Orders>>() {}
        );
    }

    public void updatePredictedArrival(Long orderId, LocalDateTime predictedArrivalAt) {
        exchange(
            "/deliveries/" + orderId + "/predicted-arrival",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("predictedArrivalAt", predictedArrivalAt)),
            new ParameterizedTypeReference<Result<Orders>>() {}
        );
    }

    public void updateDeliverySequence(String riderId, List<Map<String, Object>> items) {
        exchange(
            "/riders/" + riderId + "/delivery-sequence",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("items", items)),
            new ParameterizedTypeReference<Result<List<Orders>>>() {}
        );
    }

    public void recordLifecycle(
        Long orderId,
        String eventType,
        String fromStatus,
        String fromDeliveryStatus,
        String actorRole,
        String actorId,
        String remark
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("eventType", eventType);
        body.put("fromStatus", fromStatus);
        body.put("fromDeliveryStatus", fromDeliveryStatus);
        body.put("actorRole", actorRole);
        body.put("actorId", actorId);
        body.put("remark", remark);
        exchange(
            "/lifecycle-events",
            HttpMethod.POST,
            new HttpEntity<>(body),
            new ParameterizedTypeReference<Result<Void>>() {}
        );
    }

    private Orders actorCommand(Long orderId, String action, String riderId) {
        return exchange(
            "/deliveries/" + orderId + "/" + action,
            HttpMethod.POST,
            new HttpEntity<>(Map.of("riderId", riderId)),
            new ParameterizedTypeReference<Result<Orders>>() {}
        );
    }

    private <T> T get(String path, ParameterizedTypeReference<Result<T>> type) {
        return exchange(path, HttpMethod.GET, null, type);
    }

    private <T> T exchange(
        String path,
        HttpMethod method,
        HttpEntity<?> entity,
        ParameterizedTypeReference<Result<T>> type
    ) {
        try {
            ResponseEntity<Result<T>> response = restTemplate.exchange(
                serviceEndpoints.order() + "/internal/order/v1" + path,
                method,
                entity,
                type
            );
            Result<T> body = response.getBody();
            return body == null ? null : body.data();
        } catch (HttpStatusCodeException exception) {
            throw mapped(exception);
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    private BusinessException mapped(HttpStatusCodeException exception) {
        int status = exception.getStatusCode().value();
        String message = "订单服务暂不可用，请稍后重试";
        String errorCode = DomainErrorCode.UPSTREAM_UNAVAILABLE;
        try {
            JsonNode node = objectMapper.readTree(exception.getResponseBodyAsString());
            if (node != null) {
                if (node.path("message").isTextual() && !node.path("message").asText().isBlank()) {
                    message = node.path("message").asText();
                }
                if (node.path("errorCode").isTextual() && !node.path("errorCode").asText().isBlank()) {
                    errorCode = node.path("errorCode").asText();
                }
            }
        } catch (Exception ignored) {
        }
        if (status >= 400 && status < 500) {
            return new BusinessException(status, message, errorCode);
        }
        return upstreamUnavailable();
    }

    private BusinessException upstreamUnavailable() {
        return new BusinessException(503, "订单服务暂不可用，请稍后重试", DomainErrorCode.UPSTREAM_UNAVAILABLE);
    }

    private String statsPath(String path, LocalDate startDate, LocalDate endDate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        if (startDate != null) {
            builder.queryParam("startDate", startDate);
        }
        if (endDate != null) {
            builder.queryParam("endDate", endDate);
        }
        return builder.build().toUriString();
    }

    private String adminOrderPath(
        String path,
        Integer page,
        Integer size,
        String status,
        String deliveryStatus,
        LocalDate startDate,
        LocalDate endDate,
        String keyword
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        if (page != null) {
            builder.queryParam("page", page);
        }
        if (size != null) {
            builder.queryParam("size", size);
        }
        if (status != null && !status.isBlank()) {
            builder.queryParam("status", status);
        }
        if (deliveryStatus != null && !deliveryStatus.isBlank()) {
            builder.queryParam("deliveryStatus", deliveryStatus);
        }
        if (startDate != null) {
            builder.queryParam("startDate", startDate);
        }
        if (endDate != null) {
            builder.queryParam("endDate", endDate);
        }
        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("keyword", keyword);
        }
        return builder.build().toUriString();
    }

    public record CompletedOrderStats(long count, long totalPrice) {
        public static final CompletedOrderStats EMPTY = new CompletedOrderStats(0, 0);

        public CompletedOrderStats add(Integer totalPrice) {
            int price = totalPrice == null ? 0 : totalPrice;
            return new CompletedOrderStats(count + 1, this.totalPrice + price);
        }

        public int averagePrice() {
            if (count <= 0) {
                return 0;
            }
            return (int) Math.round((double) totalPrice / count);
        }
    }
}
