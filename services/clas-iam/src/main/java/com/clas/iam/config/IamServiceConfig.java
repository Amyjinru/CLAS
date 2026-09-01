package com.clas.iam.config;

import com.clas.common.service.ServiceIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IamServiceConfig {
    @Bean
    ServiceIdentity serviceIdentity() {
        return new ServiceIdentity("clas-iam", "Identity and Account");
    }
}
