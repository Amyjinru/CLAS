package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
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
public class CatalogClient {
    private final RestTemplate restTemplate;
    private final ServiceEndpoints serviceEndpoints;

    public CatalogClient(RestTemplate restTemplate, ServiceEndpoints serviceEndpoints) {
        this.restTemplate = restTemplate;
        this.serviceEndpoints = serviceEndpoints;
    }

    public Map<Long, Integer> getProductAveragePrices(Collection<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        String ids = merchantIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        Map<Long, Integer> averages = get(
            "/merchants/product-average-prices?ids=" + ids,
            new ParameterizedTypeReference<Result<Map<Long, Integer>>>() {}
        );
        return averages == null ? Map.of() : averages;
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
