package com.clas.compat.config;

import com.clas.common.service.ServiceIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompatServiceConfig {
    @Bean
    ServiceIdentity serviceIdentity() {
        return new ServiceIdentity("clas-compat", "Rider and Operations Compat");
    }
}
