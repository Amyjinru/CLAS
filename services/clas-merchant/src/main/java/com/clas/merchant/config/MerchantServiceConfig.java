package com.clas.merchant.config;

import com.clas.common.service.ServiceIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MerchantServiceConfig {
    @Bean
    ServiceIdentity serviceIdentity() {
        return new ServiceIdentity("clas-merchant", "Merchant");
    }
}
