package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.IamClient;
import com.clas.client.OrderClient;
import com.clas.entity.Merchant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecommendService {
    private static final double W_PURCHASE = 0.32;
    private static final double W_SCORE = 0.23;
    private static final double W_REVIEW = 0.14;
    private static final double W_FAVORITE = 0.14;
    private static final double W_PRICE = 0.09;
    private static final double W_LOGO = 0.08;

    private final OrderClient orderClient;
    private final IamClient iamClient;

    public RecommendService(OrderClient orderClient, IamClient iamClient) {
        this.orderClient = orderClient;
        this.iamClient = iamClient;
    }

    public List<Merchant> sortByRecommend(List<Merchant> merchants, String userId) {
        if (merchants.isEmpty()) {
            return merchants;
        }
        List<Long> merchantIds = merchants.stream().map(Merchant::getId).toList();
        Map<Long, Integer> purchaseCounts = orderClient.getUserPurchaseCounts(userId);
        Map<Long, Long> favoriteCounts = favoriteCounts(merchantIds);
        Map<Long, Long> reviewCounts = orderClient.getReviewCounts(merchantIds);
        int maxPurchase = purchaseCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        long maxFavoriteLong = favoriteCounts.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        long maxReviewLong = reviewCounts.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        int maxFavorite = (int) maxFavoriteLong;
        int maxReview = (int) maxReviewLong;
        int maxPrice = merchants.stream().mapToInt(m -> m.getAveragePrice() == null ? 0 : m.getAveragePrice()).max().orElse(0);

        return merchants.stream()
            .sorted((left, right) -> Double.compare(
                recommendScore(right, userId, purchaseCounts, favoriteCounts, reviewCounts, maxPurchase, maxFavorite, maxReview, maxPrice),
                recommendScore(left, userId, purchaseCounts, favoriteCounts, reviewCounts, maxPurchase, maxFavorite, maxReview, maxPrice)
            ))
            .collect(Collectors.toList());
    }

    private double recommendScore(
        Merchant merchant,
        String userId,
        Map<Long, Integer> purchaseCounts,
        Map<Long, Long> favoriteCounts,
        Map<Long, Long> reviewCounts,
        int maxPurchase,
        int maxFavorite,
        int maxReview,
        int maxPrice
    ) {
        double purchaseNorm = normalize(purchaseCounts.getOrDefault(merchant.getId(), 0), maxPurchase);
        double scoreNorm = merchant.getScore() == null ? 0 : merchant.getScore().doubleValue() / 5.0;
        double reviewNorm = normalize(reviewCounts.getOrDefault(merchant.getId(), 0L).intValue(), maxReview);
        double favoriteNorm = normalize(favoriteCounts.getOrDefault(merchant.getId(), 0L).intValue(), maxFavorite);
        double logoNorm = hasCustomLogo(merchant) ? 1.0 : 0.0;
        int price = merchant.getAveragePrice() == null ? maxPrice : merchant.getAveragePrice();
        double priceNorm = maxPrice <= 0 ? 0.5 : 1.0 - ((double) price / maxPrice);

        if (userId == null || userId.isBlank()) {
            return W_SCORE * scoreNorm
                + W_REVIEW * reviewNorm
                + W_FAVORITE * favoriteNorm
                + W_PRICE * priceNorm
                + W_LOGO * logoNorm;
        }
        return W_PURCHASE * purchaseNorm
            + W_SCORE * scoreNorm
            + W_REVIEW * reviewNorm
            + W_FAVORITE * favoriteNorm
            + W_PRICE * priceNorm
            + W_LOGO * logoNorm;
    }

    private Map<Long, Long> favoriteCounts(List<Long> merchantIds) {
        Map<Long, Long> counts = new HashMap<>();
        for (Long merchantId : merchantIds) {
            counts.put(merchantId, iamClient.favoriteCount(merchantId));
        }
        return counts;
    }

    private boolean hasCustomLogo(Merchant merchant) {
        String logo = merchant.getLogo();
        return logo != null && !logo.isBlank();
    }

    private double normalize(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        return (double) value / max;
    }
}
