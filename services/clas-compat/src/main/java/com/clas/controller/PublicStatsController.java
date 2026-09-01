package com.clas.controller;

import com.clas.common.Result;
import com.clas.client.CatalogClient;
import com.clas.client.IamClient;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共统计端点 — 供落地页等无需认证的场景使用
 */
@RestController
@RequestMapping("/api/public")
public class PublicStatsController {

    private final CatalogClient catalogClient;
    private final IamClient iamClient;

    public PublicStatsController(CatalogClient catalogClient, IamClient iamClient) {
        this.catalogClient = catalogClient;
        this.iamClient = iamClient;
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        Map<String, Long> catalogStats = catalogClient.getPublicStats();
        Map<String, Long> iamStats = iamClient.getPublicStats();
        long merchants = catalogStats.getOrDefault("merchants", 0L);
        long products = catalogStats.getOrDefault("products", 0L);
        long users = iamStats.getOrDefault("users", 0L);
        return Result.ok(Map.of("merchants", merchants, "products", products, "users", users));
    }
}
