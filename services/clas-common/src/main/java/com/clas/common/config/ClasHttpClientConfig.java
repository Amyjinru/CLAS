package com.clas.common.config;

import com.clas.common.client.ServiceEndpoints;
import com.clas.common.service.ServiceIdentity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ServiceEndpoints.class)
public class ClasHttpClientConfig {
    @Bean
    public RestTemplate clasRestTemplate(
        ObjectProvider<ServiceIdentity> serviceIdentityProvider,
        @Value("${clas.internal-api-key:}") String internalApiKey
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(2000);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add((request, body, execution) -> {
            if (request.getURI().getPath().startsWith("/internal/")) {
                ServiceIdentity serviceIdentity = serviceIdentityProvider.getIfAvailable();
                if (serviceIdentity != null) {
                    request.getHeaders().set("X-CLAS-Service", serviceIdentity.id());
                }
                if (!internalApiKey.isBlank()) {
                    request.getHeaders().set("X-CLAS-Internal-Key", internalApiKey);
                }
                if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                    String authorization = attributes.getRequest().getHeader("Authorization");
                    if (authorization != null && !authorization.isBlank()) {
                        request.getHeaders().set("Authorization", authorization);
                    }
                    String requestId = attributes.getRequest().getHeader("X-Request-Id");
                    if (requestId != null && !requestId.isBlank()) {
                        request.getHeaders().set("X-Request-Id", requestId);
                    }
                }
            }
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
