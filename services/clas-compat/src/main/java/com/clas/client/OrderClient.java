package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.dto.MerchantStatsDTO;
import com.clas.entity.Orders;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
