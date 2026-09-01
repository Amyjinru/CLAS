package com.clas.controller;

import com.clas.common.Result;
import com.clas.client.CatalogClient;
import com.clas.client.IamClient;
import com.clas.client.MerchantClient;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicStatsController {

    private final CatalogClient catalogClient;
    private final MerchantClient merchantClient;
    private final IamClient iamClient;

    public PublicStatsController(CatalogClient catalogClient, MerchantClient merchantClient, IamClient iamClient) {
        this.catalogClient = catalogClient;
        this.merchantClient = merchantClient;
        this.iamClient = iamClient;
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        Map<String, Long> catalogStats = catalogClient.getPublicStats();
        Map<String, Long> merchantStats = merchantClient.getPublicStats();
        Map<String, Long> iamStats = iamClient.getPublicStats();
        long merchants = merchantStats.getOrDefault("merchants", 0L);
        long products = catalogStats.getOrDefault("products", 0L);
        long users = iamStats.getOrDefault("users", 0L);
        return Result.ok(Map.of("merchants", merchants, "products", products, "users", users));
    }
}
