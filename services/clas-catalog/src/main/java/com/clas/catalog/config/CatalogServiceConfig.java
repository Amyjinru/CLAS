package com.clas.catalog.config;

import com.clas.common.service.ServiceIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogServiceConfig {
    @Bean
    ServiceIdentity serviceIdentity() {
        return new ServiceIdentity("clas-catalog", "Catalog");
    }
}
