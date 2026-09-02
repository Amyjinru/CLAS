package com.clas.dto;

public record OrderDashboardStats(
    Long totalOrders,
    Long totalSales,
    Long todayOrders,
    Long todaySales,
    Long pendingPaymentOrders,
    Long paidOrders,
    Long completedOrders
) {
}
