package com.clas.common.config;

import com.clas.common.client.ServiceEndpoints;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ServiceEndpoints.class)
public class ClasHttpClientConfig {
    @Bean
    public RestTemplate clasRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(2000);
        return new RestTemplate(factory);
    }
}
