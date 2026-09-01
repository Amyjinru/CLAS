package com.clas.dto;

import java.util.List;

/**
 * 订单统计数据
 */
public record OrderStatsDTO(
    /** 各状态订单数量 */
    List<StatusCount> statusCounts,
    /** 近7天每日订单数 */
    List<DailyCount> dailyOrders
) {
    public record StatusCount(String status, Long count) {}
    public record DailyCount(String date, Long count, Long amount) {}
}
