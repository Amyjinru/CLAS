package com.clas.client;

import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.dto.RoleApplicationRecordResponse;
import java.util.List;
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

    public List<RoleApplicationRecordResponse> listRiderApplicationRecords(String userId) {
        try {
            ResponseEntity<Result<List<RoleApplicationRecordResponse>>> response = restTemplate.exchange(
                serviceEndpoints.compat() + "/internal/compat/v1/users/" + userId + "/rider-application-records",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            Result<List<RoleApplicationRecordResponse>> body = response.getBody();
            return body == null || body.data() == null ? List.of() : body.data();
        } catch (RestClientException exception) {
            return List.of();
        }
    }
}
