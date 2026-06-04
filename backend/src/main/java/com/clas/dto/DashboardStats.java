package com.clas.dto;

import java.math.BigDecimal;

/**
 * 管理员仪表盘汇总数据
 */
public record DashboardStats(
    /** 总用户数 */
    Long totalUsers,
    /** 总商家数 */
    Long totalMerchants,
    /** 总订单数 */
    Long totalOrders,
    /** 总销售额（分） */
    Long totalSales,
    /** 今日新增订单数 */
    Long todayOrders,
    /** 今日销售额（分） */
    Long todaySales,
    /** 待支付订单数 */
    Long pendingPaymentOrders,
    /** 已支付待接单数 */
    Long paidOrders,
    /** 已完成订单数 */
    Long completedOrders
) {
}
