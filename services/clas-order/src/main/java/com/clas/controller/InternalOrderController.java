package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.MerchantStatsDTO;
import com.clas.service.OrderStatisticsService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/order/v1")
public class InternalOrderController {
    private final OrderStatisticsService statisticsService;

    public InternalOrderController(OrderStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/merchants/completed-order-stats")
    public Result<Map<Long, OrderStatisticsService.CompletedOrderStats>> completedOrderStats(@RequestParam String ids) {
        List<Long> merchantIds = parseIds(ids);
        return Result.ok(statisticsService.getCompletedOrderStats(merchantIds));
    }

    @GetMapping("/users/{userId}/purchase-counts")
    public Result<Map<Long, Integer>> purchaseCounts(@PathVariable String userId) {
        return Result.ok(statisticsService.getUserPurchaseCounts(userId));
    }

    @GetMapping("/merchants/review-counts")
    public Result<Map<Long, Long>> reviewCounts(@RequestParam String ids) {
        List<Long> merchantIds = parseIds(ids);
        return Result.ok(statisticsService.getReviewCounts(merchantIds));
    }

    @GetMapping("/merchants/{merchantId}/stats")
    public Result<MerchantStatsDTO> merchantStats(@PathVariable Long merchantId) {
        return Result.ok(statisticsService.getMerchantStats(merchantId));
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(Long::valueOf)
            .collect(Collectors.toList());
    }
}
