package com.clas.common.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clas.services")
public record ServiceEndpoints(
    String iam,
    String catalog,
    String order,
    String compat
) {
    public ServiceEndpoints {
        if (iam == null || iam.isBlank()) {
            iam = "http://clas-iam:8081";
        }
        if (catalog == null || catalog.isBlank()) {
            catalog = "http://clas-catalog:8082";
        }
        if (order == null || order.isBlank()) {
            order = "http://clas-order:8083";
        }
        if (compat == null || compat.isBlank()) {
            compat = "http://clas-compat:8084";
        }
    }
}
