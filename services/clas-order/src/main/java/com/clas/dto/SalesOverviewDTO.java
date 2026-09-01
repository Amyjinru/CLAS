package com.clas.dto;

import java.util.List;

/**
 * 销售额概览数据
 */
public record SalesOverviewDTO(
    /** 近7天每日销售额 */
    List<DailySale> dailySales,
    /** 总销售额（分） */
    Long totalSales,
    /** 月销售额（分） */
    Long monthlySales,
    /** 周销售额（分） */
    Long weeklySales
) {
    public record DailySale(String date, Long amount, Long orderCount) {}
}
