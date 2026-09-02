package com.clas.dto;

public record ProductSalesRank(
    Long productId,
    Long soldCount,
    Long totalAmount
) {
}
