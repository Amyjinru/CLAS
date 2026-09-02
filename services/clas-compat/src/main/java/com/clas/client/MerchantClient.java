package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.config.UserContext;
import com.clas.entity.Merchant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class MerchantClient {
    private final RestTemplate restTemplate;
    private final ServiceEndpoints serviceEndpoints;

    public MerchantClient(RestTemplate restTemplate, ServiceEndpoints serviceEndpoints) {
        this.restTemplate = restTemplate;
        this.serviceEndpoints = serviceEndpoints;
    }

    public Merchant getMerchant(Long merchantId) {
        return get("/merchants/" + merchantId, new ParameterizedTypeReference<Result<Merchant>>() {});
    }

    public Map<Long, Merchant> getMerchants(Collection<Long> merchantIds) {
        if (merchantIds.isEmpty()) {
            return Map.of();
        }
        String ids = merchantIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Merchant> merchants = get("/merchants/batch?ids=" + ids, new ParameterizedTypeReference<Result<List<Merchant>>>() {});
        if (merchants == null) {
            return Map.of();
        }
        return merchants.stream().collect(Collectors.toMap(Merchant::getId, Function.identity()));
    }

    public Long getMerchantIdByUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return get("/users/" + userId + "/merchant-id", new ParameterizedTypeReference<Result<Long>>() {});
    }

    public Long getCurrentMerchantId() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        Long merchantId = getMerchantIdByUser(userId);
        if (merchantId == null) {
            throw new BusinessException("当前用户未入驻为商家");
        }
        return merchantId;
    }

    public Map<String, Long> getPublicStats() {
        Map<String, Long> stats = get("/stats/public", new ParameterizedTypeReference<Result<Map<String, Long>>>() {});
        return stats == null ? Map.of() : stats;
    }

    public Map<String, Long> getAdminStats() {
        Map<String, Long> stats = get("/stats/admin", new ParameterizedTypeReference<Result<Map<String, Long>>>() {});
        return stats == null ? Map.of() : stats;
    }

    public List<Merchant> topByScore(int limit) {
        List<Merchant> merchants = get(
            "/merchants/top-by-score?limit=" + limit,
            new ParameterizedTypeReference<Result<List<Merchant>>>() {}
        );
        return merchants == null ? List.of() : merchants;
    }

    private <T> T get(String path, ParameterizedTypeReference<Result<T>> type) {
        try {
            ResponseEntity<Result<T>> response = restTemplate.exchange(
                serviceEndpoints.merchant() + "/internal/merchant/v1" + path,
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
        return new BusinessException(503, "商家服务暂不可用，请稍后重试", DomainErrorCode.UPSTREAM_UNAVAILABLE);
    }
}
