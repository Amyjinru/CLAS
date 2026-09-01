package com.clas.client;

import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
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

    public boolean hasPendingRiderApplication(String userId) {
        try {
            ResponseEntity<Result<Boolean>> response = restTemplate.exchange(
                serviceEndpoints.compat() + "/internal/compat/v1/users/" + userId + "/rider-pending",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            Result<Boolean> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.data());
        } catch (RestClientException exception) {
            return false;
        }
    }
}
