package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.dto.StockChangeRequest;
import com.clas.entity.GroupDeal;
import com.clas.entity.Product;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
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
        if (productIds.isEmpty()) {
            return Map.of();
        }
        String ids = productIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Product> products = get("/products/batch?ids=" + ids, new ParameterizedTypeReference<Result<List<Product>>>() {});
        if (products == null) {
            return Map.of();
        }
        return products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    public boolean deductProductStock(Long productId, Integer quantity) {
        Boolean ok = post("/products/" + productId + "/deduct-stock",
            new StockChangeRequest(quantity),
            new ParameterizedTypeReference<Result<Boolean>>() {});
        return Boolean.TRUE.equals(ok);
    }

    public void restoreProductStock(Long productId, Integer quantity) {
        postVoid("/products/" + productId + "/restore-stock", new StockChangeRequest(quantity));
    }

    public GroupDeal getDeal(Long dealId) {
        return get("/deals/" + dealId, new ParameterizedTypeReference<Result<GroupDeal>>() {});
    }

    public Map<Long, GroupDeal> getDeals(Collection<Long> dealIds) {
        if (dealIds.isEmpty()) {
            return Map.of();
        }
        String ids = dealIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<GroupDeal> deals = get("/deals/batch?ids=" + ids, new ParameterizedTypeReference<Result<List<GroupDeal>>>() {});
        if (deals == null) {
            return Map.of();
        }
        return deals.stream().collect(Collectors.toMap(GroupDeal::getId, Function.identity()));
    }

    public boolean deductDealStock(Long dealId) {
        Boolean ok = post("/deals/" + dealId + "/deduct-stock", null, new ParameterizedTypeReference<Result<Boolean>>() {});
        return Boolean.TRUE.equals(ok);
    }

    public void restoreDealStock(Long dealId) {
        postVoid("/deals/" + dealId + "/restore-stock", null);
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

    private <T> T post(String path, Object payload, ParameterizedTypeReference<Result<T>> type) {
        try {
            ResponseEntity<Result<T>> response = restTemplate.exchange(
                serviceEndpoints.catalog() + "/internal/catalog/v1" + path,
                HttpMethod.POST,
                new HttpEntity<>(payload),
                type
            );
            Result<T> body = response.getBody();
            return body == null ? null : body.data();
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    private void postVoid(String path, Object payload) {
        try {
            restTemplate.exchange(
                serviceEndpoints.catalog() + "/internal/catalog/v1" + path,
                HttpMethod.POST,
                new HttpEntity<>(payload),
                Void.class
            );
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    private BusinessException upstreamUnavailable() {
        return new BusinessException(503, "商品目录服务暂不可用，请稍后重试", DomainErrorCode.UPSTREAM_UNAVAILABLE);
    }
}
