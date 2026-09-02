package com.clas.dto;

public record MerchantSalesRank(
    Long merchantId,
    Long totalSales,
    Long orderCount
) {
}
