package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.MerchantStatusEnum;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.config.UserContext;
import com.clas.entity.Merchant;
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

    public Merchant requireOpenMerchant(Long merchantId) {
        Merchant merchant = getMerchant(merchantId);
        if (merchant == null || merchant.getStatus() != MerchantStatusEnum.OPEN) {
            throw new BusinessException("商家不存在或未营业");
        }
        return merchant;
    }

    public Long getCurrentMerchantId() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        Long merchantId = get("/users/" + userId + "/merchant-id", new ParameterizedTypeReference<Result<Long>>() {});
        if (merchantId == null) {
            throw new BusinessException("当前用户未入驻为商家");
        }
        return merchantId;
    }

    public void refreshAveragePrice(Long merchantId) {
        postVoid("/merchants/" + merchantId + "/refresh-average-price", null);
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

    private void postVoid(String path, Object payload) {
        try {
            restTemplate.postForEntity(
                serviceEndpoints.merchant() + "/internal/merchant/v1" + path,
                payload,
                Void.class
            );
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    private BusinessException upstreamUnavailable() {
        return new BusinessException(503, "商家服务暂不可用，请稍后重试", DomainErrorCode.UPSTREAM_UNAVAILABLE);
    }
}
