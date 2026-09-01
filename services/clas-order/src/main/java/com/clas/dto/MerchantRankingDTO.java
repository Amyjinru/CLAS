package com.clas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商家排行数据
 */
public record MerchantRankingDTO(
    /** 按销售额排行 */
    List<MerchantRank> bySales,
    /** 按评分排行 */
    List<MerchantRank> byRating
) {
    public record MerchantRank(
        Long merchantId,
        String merchantName,
        String category,
        BigDecimal score,
        Long totalSales,
        Long orderCount
    ) {}
}
