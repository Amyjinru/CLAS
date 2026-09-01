package com.clas.dto;

import java.util.List;

/**
 * 热销商品排行
 */
public record TopProductDTO(
    List<ProductRank> products
) {
    public record ProductRank(
        Long productId,
        String productName,
        String merchantName,
        Long soldCount,
        Long totalAmount
    ) {}
}
