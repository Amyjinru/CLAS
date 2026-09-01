package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.common.dto.InternalTokenValidationRequest;
import com.clas.common.dto.InternalValidatedUser;
import com.clas.dto.InternalNotificationRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class IamClient {
    private final RestTemplate restTemplate;
    private final ServiceEndpoints serviceEndpoints;

    public IamClient(RestTemplate restTemplate, ServiceEndpoints serviceEndpoints) {
        this.restTemplate = restTemplate;
        this.serviceEndpoints = serviceEndpoints;
    }

    public InternalValidatedUser validateToken(String token) {
        try {
            ResponseEntity<Result<InternalValidatedUser>> response = restTemplate.exchange(
                serviceEndpoints.iam() + "/internal/iam/v1/auth/validate", HttpMethod.POST,
                new HttpEntity<>(new InternalTokenValidationRequest(token)),
                new ParameterizedTypeReference<Result<InternalValidatedUser>>() {}
            );
            Result<InternalValidatedUser> body = response.getBody();
            return body == null ? null : body.data();
        } catch (HttpStatusCodeException exception) {
            int status = exception.getStatusCode().value();
            throw new BusinessException(status,
                status == 401 ? "登录已过期，请重新登录" : "当前身份尚未审核通过或已不可用");
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    public void assertCanUsePlatform(String userId) {
        try {
            restTemplate.exchange(
                serviceEndpoints.iam() + "/internal/iam/v1/users/" + userId + "/platform-access",
                HttpMethod.GET,
                null,
                Void.class
            );
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    public void sendNotification(InternalNotificationRequest request) {
        try {
            restTemplate.exchange(
                serviceEndpoints.iam() + "/internal/iam/v1/notifications",
                HttpMethod.POST,
                new HttpEntity<>(request),
                Void.class
            );
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    private BusinessException upstreamUnavailable() {
        return new BusinessException(503, "账户服务暂不可用，请稍后重试", DomainErrorCode.UPSTREAM_UNAVAILABLE);
    }
}
