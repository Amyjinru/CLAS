package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.dto.MerchantStatsDTO;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderClient {
    private final RestTemplate restTemplate;
    private final ServiceEndpoints serviceEndpoints;

    public OrderClient(RestTemplate restTemplate, ServiceEndpoints serviceEndpoints) {
        this.restTemplate = restTemplate;
        this.serviceEndpoints = serviceEndpoints;
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

    private <T> T get(String path, ParameterizedTypeReference<Result<T>> type) {
        try {
            ResponseEntity<Result<T>> response = restTemplate.exchange(
                serviceEndpoints.order() + "/internal/order/v1" + path,
                HttpMethod.GET,
                null,
                type
            );
            Result<T> body = response.getBody();
            return body == null ? null : body.data();
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
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
