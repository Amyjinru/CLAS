package com.clas.service;

import com.clas.client.CatalogClient;
import com.clas.client.IamClient;
import com.clas.client.MerchantClient;
import com.clas.client.OrderClient;
import com.clas.dto.DashboardStats;
import com.clas.dto.MerchantRankingDTO;
import com.clas.dto.MerchantSalesRank;
import com.clas.dto.MerchantStatsDTO;
import com.clas.dto.OrderDashboardStats;
import com.clas.dto.OrderStatsDTO;
import com.clas.dto.ProductSalesRank;
import com.clas.dto.SalesOverviewDTO;
import com.clas.dto.TopProductDTO;
import com.clas.entity.Merchant;
import com.clas.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {
    private final IamClient iamClient;
    private final MerchantClient merchantClient;
    private final CatalogClient catalogClient;
    private final OrderClient orderClient;

    public StatisticsService(
        IamClient iamClient,
        MerchantClient merchantClient,
        CatalogClient catalogClient,
        OrderClient orderClient
    ) {
        this.iamClient = iamClient;
        this.merchantClient = merchantClient;
        this.catalogClient = catalogClient;
        this.orderClient = orderClient;
    }

    public DashboardStats getDashboardStats() {
        return getDashboardStats(null, null);
    }

    public DashboardStats getDashboardStats(LocalDate startDate, LocalDate endDate) {
        long totalUsers = iamClient.getPublicStats().getOrDefault("users", 0L);
        long totalMerchants = merchantClient.getAdminStats().getOrDefault("merchants", 0L);
        OrderDashboardStats orderStats = orderClient.getDashboardStats(startDate, endDate);
        return new DashboardStats(
            totalUsers,
            totalMerchants,
            orderStats.totalOrders(),
            orderStats.totalSales(),
            orderStats.todayOrders(),
            orderStats.todaySales(),
            orderStats.pendingPaymentOrders(),
            orderStats.paidOrders(),
            orderStats.completedOrders()
        );
    }

    public OrderStatsDTO getOrderStats() {
        return getOrderStats(null, null);
    }

    public OrderStatsDTO getOrderStats(LocalDate startDate, LocalDate endDate) {
        return orderClient.getOrderStats(startDate, endDate);
    }

    public SalesOverviewDTO getSalesOverview() {
        return getSalesOverview(null, null);
    }

    public SalesOverviewDTO getSalesOverview(LocalDate startDate, LocalDate endDate) {
        return orderClient.getSalesOverview(startDate, endDate);
    }

    public MerchantRankingDTO getMerchantRanking() {
        List<MerchantSalesRank> salesRanks = orderClient.getMerchantSalesRanking();
        Map<Long, Merchant> salesMerchants = merchantClient.getMerchants(
            salesRanks.stream().map(MerchantSalesRank::merchantId).toList()
        );
        List<MerchantRankingDTO.MerchantRank> bySales = salesRanks.stream()
            .map(rank -> toMerchantRank(rank.merchantId(), salesMerchants.get(rank.merchantId()), rank))
            .toList();

        List<Merchant> topRated = merchantClient.topByScore(10);
        Map<Long, MerchantSalesRank> ratingSales = orderClient.getMerchantSales(
            topRated.stream().map(Merchant::getId).toList()
        );
        List<MerchantRankingDTO.MerchantRank> byRating = topRated.stream()
            .map(merchant -> toMerchantRank(
                merchant.getId(),
                merchant,
                ratingSales.getOrDefault(merchant.getId(), new MerchantSalesRank(merchant.getId(), 0L, 0L))
            ))
            .toList();
        return new MerchantRankingDTO(bySales, byRating);
    }

    public TopProductDTO getTopProducts() {
        List<ProductSalesRank> ranks = orderClient.getProductSalesRanking();
        Map<Long, Product> products = catalogClient.getProducts(
            ranks.stream().map(ProductSalesRank::productId).toList()
        );
        Map<Long, Merchant> merchants = merchantClient.getMerchants(
            products.values().stream().map(Product::getMerchantId).distinct().toList()
        );
        List<TopProductDTO.ProductRank> productRanks = ranks.stream()
            .map(rank -> {
                Product product = products.get(rank.productId());
                Merchant merchant = product == null ? null : merchants.get(product.getMerchantId());
                return new TopProductDTO.ProductRank(
                    rank.productId(),
                    product == null ? "商品#" + rank.productId() : product.getName(),
                    merchant == null ? null : merchant.getMerchantName(),
                    rank.soldCount(),
                    rank.totalAmount()
                );
            })
            .toList();
        return new TopProductDTO(productRanks);
    }

    public MerchantStatsDTO getMerchantStats(Long merchantId) {
        return orderClient.getMerchantStats(merchantId);
    }

    private MerchantRankingDTO.MerchantRank toMerchantRank(Long merchantId, Merchant merchant, MerchantSalesRank rank) {
        return new MerchantRankingDTO.MerchantRank(
            merchantId,
            merchant == null ? "商家#" + merchantId : merchant.getMerchantName(),
            merchant == null ? null : merchant.getCategory(),
            merchant == null ? BigDecimal.ZERO : merchant.getScore(),
            rank == null ? 0L : rank.totalSales(),
            rank == null ? 0L : rank.orderCount()
        );
    }
}
