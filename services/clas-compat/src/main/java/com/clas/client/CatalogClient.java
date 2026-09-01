package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.entity.Product;
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
public class CatalogClient {
    private final RestTemplate restTemplate;
    private final ServiceEndpoints serviceEndpoints;

    public CatalogClient(RestTemplate restTemplate, ServiceEndpoints serviceEndpoints) {
        this.restTemplate = restTemplate;
        this.serviceEndpoints = serviceEndpoints;
    }

    public Product getProduct(Long productId) {
        return get("/products/" + productId, new ParameterizedTypeReference<Result<Product>>() {});
    }

    public Map<Long, Product> getProducts(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        String ids = productIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Product> products = get("/products/batch?ids=" + ids, new ParameterizedTypeReference<Result<List<Product>>>() {});
        if (products == null) {
            return Map.of();
        }
        return products.stream().collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
    }

    public Map<String, Long> getPublicStats() {
        Map<String, Long> stats = get("/stats/public", new ParameterizedTypeReference<Result<Map<String, Long>>>() {});
        return stats == null ? Map.of() : stats;
    }

    private <T> T get(String path, ParameterizedTypeReference<Result<T>> type) {
        try {
            ResponseEntity<Result<T>> response = restTemplate.exchange(
                serviceEndpoints.catalog() + "/internal/catalog/v1" + path,
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
        return new BusinessException(503, "商品目录服务暂不可用，请稍后重试", DomainErrorCode.UPSTREAM_UNAVAILABLE);
    }
}
