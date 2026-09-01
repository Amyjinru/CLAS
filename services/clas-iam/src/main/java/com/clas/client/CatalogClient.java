package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.dto.RoleApplicationRecordResponse;
import com.clas.entity.Merchant;
import java.util.List;
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

    public Merchant requireMerchant(Long merchantId) {
        Merchant merchant = getMerchant(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        return merchant;
    }

    public Merchant getMerchant(Long merchantId) {
        try {
            ResponseEntity<Result<Merchant>> response = restTemplate.exchange(
                serviceEndpoints.catalog() + "/internal/catalog/v1/merchants/" + merchantId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            Result<Merchant> body = response.getBody();
            return body == null ? null : body.data();
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    public List<Merchant> getMerchantsByIds(List<Long> merchantIds) {
        if (merchantIds.isEmpty()) {
            return List.of();
        }
        try {
            ResponseEntity<Result<List<Merchant>>> response = restTemplate.exchange(
                serviceEndpoints.catalog() + "/internal/catalog/v1/merchants/batch?ids="
                    + String.join(",", merchantIds.stream().map(String::valueOf).toList()),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            Result<List<Merchant>> body = response.getBody();
            return body == null || body.data() == null ? List.of() : body.data();
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    public boolean hasPendingMerchantApplication(String userId) {
        try {
            ResponseEntity<Result<Boolean>> response = restTemplate.exchange(
                serviceEndpoints.catalog() + "/internal/catalog/v1/users/" + userId + "/merchant-pending",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            Result<Boolean> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.data());
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    public RoleApplicationRecordResponse getMerchantApplicationRecord(String userId) {
        try {
            ResponseEntity<Result<RoleApplicationRecordResponse>> response = restTemplate.exchange(
                serviceEndpoints.catalog() + "/internal/catalog/v1/users/" + userId + "/merchant-application-record",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            Result<RoleApplicationRecordResponse> body = response.getBody();
            return body == null ? null : body.data();
        } catch (RestClientException exception) {
            return null;
        }
    }

    private BusinessException upstreamUnavailable() {
        return new BusinessException(503, "商家服务暂不可用，请稍后重试", DomainErrorCode.UPSTREAM_UNAVAILABLE);
    }
}
