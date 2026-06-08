package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.entity.Favorite;
import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import com.clas.entity.Review;
import com.clas.mapper.FavoriteMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ReviewMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecommendService {
    private static final double W_PURCHASE = 0.35;
    private static final double W_SCORE = 0.25;
    private static final double W_REVIEW = 0.15;
    private static final double W_FAVORITE = 0.15;
    private static final double W_PRICE = 0.10;

    private final OrdersMapper ordersMapper;
    private final FavoriteMapper favoriteMapper;
    private final ReviewMapper reviewMapper;

    public RecommendService(OrdersMapper ordersMapper, FavoriteMapper favoriteMapper, ReviewMapper reviewMapper) {
        this.ordersMapper = ordersMapper;
        this.favoriteMapper = favoriteMapper;
        this.reviewMapper = reviewMapper;
    }

    public List<Merchant> sortByRecommend(List<Merchant> merchants, String userId) {
        if (merchants.isEmpty()) {
            return merchants;
        }
        Map<Long, Integer> purchaseCounts = purchaseCounts(userId);
        Map<Long, Long> favoriteCounts = favoriteCounts(merchants);
        Map<Long, Long> reviewCounts = reviewCounts(merchants);
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
        int price = merchant.getAveragePrice() == null ? maxPrice : merchant.getAveragePrice();
        double priceNorm = maxPrice <= 0 ? 0.5 : 1.0 - ((double) price / maxPrice);

        if (userId == null || userId.isBlank()) {
            return W_SCORE * scoreNorm + W_REVIEW * reviewNorm + W_FAVORITE * favoriteNorm + W_PRICE * priceNorm;
        }
        return W_PURCHASE * purchaseNorm
            + W_SCORE * scoreNorm
            + W_REVIEW * reviewNorm
            + W_FAVORITE * favoriteNorm
            + W_PRICE * priceNorm;
    }

    private Map<Long, Integer> purchaseCounts(String userId) {
        if (userId == null || userId.isBlank()) {
            return Map.of();
        }
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getUserId, userId)
            .in(Orders::getStatus, List.of("PAID", "ACCEPTED", "DELIVERING", "COMPLETED")));
        Map<Long, Integer> counts = new HashMap<>();
        for (Orders order : orders) {
            counts.merge(order.getMerchantId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, Long> favoriteCounts(List<Merchant> merchants) {
        List<Long> merchantIds = merchants.stream().map(Merchant::getId).toList();
        if (merchantIds.isEmpty()) {
            return Map.of();
        }
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
            .in(Favorite::getMerchantId, merchantIds));
        return favorites.stream().collect(Collectors.groupingBy(Favorite::getMerchantId, Collectors.counting()));
    }

    private Map<Long, Long> reviewCounts(List<Merchant> merchants) {
        Map<Long, Long> counts = new HashMap<>();
        for (Merchant merchant : merchants) {
            List<Long> orderIds = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getMerchantId, merchant.getId()))
                .stream()
                .map(Orders::getId)
                .toList();
            if (orderIds.isEmpty()) {
                counts.put(merchant.getId(), 0L);
                continue;
            }
            long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().in(Review::getOrderId, orderIds));
            counts.put(merchant.getId(), count);
        }
        return counts;
    }

    private double normalize(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        return (double) value / max;
    }
}
