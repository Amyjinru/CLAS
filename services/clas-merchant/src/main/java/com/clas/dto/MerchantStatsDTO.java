package com.clas.dto;

import java.util.List;

public record MerchantStatsDTO(
    Long todayOrders,
    Long todaySales,
    Long monthSales,
    Long yearSales,
    Long totalSales,
    List<DailySale> dailySales,
    List<ProductRank> topProducts
) {
    public record DailySale(String date, Long orderCount, Long amount) {}

    public record ProductRank(
        Long productId,
        String productName,
        Long soldCount,
        Long totalAmount
    ) {}
}
