package com.clas.order.config;

import com.clas.common.service.ServiceIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderServiceConfig {
    @Bean
    ServiceIdentity serviceIdentity() {
        return new ServiceIdentity("clas-order", "Order and Payment");
    }
}
