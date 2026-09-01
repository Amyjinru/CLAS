package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.entity.Orders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class CompatClient {
    private final RestTemplate restTemplate;
    private final ServiceEndpoints serviceEndpoints;

    public CompatClient(RestTemplate restTemplate, ServiceEndpoints serviceEndpoints) {
        this.restTemplate = restTemplate;
        this.serviceEndpoints = serviceEndpoints;
    }

    public void makeCommissionWithdrawable(Orders order) {
        postVoid("/orders/" + order.getId() + "/commission/withdrawable");
    }

    public void reverseCommissionForRefund(Orders order) {
        postVoid("/orders/" + order.getId() + "/commission/reverse");
    }

    public void releaseCommissionIfEligible(Orders order) {
        postVoid("/orders/" + order.getId() + "/commission/release");
    }

    private void postVoid(String path) {
        try {
            restTemplate.exchange(
                serviceEndpoints.compat() + "/internal/compat/v1" + path,
                HttpMethod.POST,
                null,
                Void.class
            );
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    private BusinessException upstreamUnavailable() {
        return new BusinessException(503, "兼容服务暂不可用，请稍后重试", DomainErrorCode.UPSTREAM_UNAVAILABLE);
    }
}
