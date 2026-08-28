package com.clas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.clas.common.MerchantStatusEnum;
import com.clas.entity.Favorite;
import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import com.clas.mapper.FavoriteMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ReviewMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendServiceTest {

    @Mock
    private OrdersMapper ordersMapper;
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private ReviewMapper reviewMapper;

    private RecommendService recommendService;

    @BeforeEach
    void setUp() {
        recommendService = new RecommendService(ordersMapper, favoriteMapper, reviewMapper);
    }

    @Test
    void anonymousUser_prefersBoostedDemoMerchantWithLogo() {
        Merchant plain = merchant(201L, "无头像小店", null, new BigDecimal("5.0"), 2500);
        Merchant boosted = merchant(101L, "演示旗舰店", "https://cdn/logo.png", new BigDecimal("4.9"), 3500);

        stubFavorites(101L, 20, 201L, 2);
        stubReviewCounts(List.of(201L, 101L), List.of(1, 18));

        List<Merchant> sorted = recommendService.sortByRecommend(List.of(plain, boosted), null);

        assertEquals(101L, sorted.get(0).getId());
    }

    @Test
    void anonymousUser_logoMerchantRanksAboveNoLogoWithSameScoreAndEngagement() {
        Merchant withoutLogo = merchant(2L, "默认头像商家", "", new BigDecimal("4.5"), 3000);
        Merchant withLogo = merchant(1L, "有图商家", "/logo.png", new BigDecimal("4.5"), 3000);

        stubFavorites(1L, 10, 2L, 10);
        stubReviewCounts(List.of(2L, 1L), List.of(8, 8));

        List<Merchant> sorted = recommendService.sortByRecommend(List.of(withoutLogo, withLogo), null);

        assertEquals(1L, sorted.get(0).getId());
    }

    @Test
    void loggedInUser_stillUsesFavoriteAndReviewSignals() {
        Merchant quiet = merchant(11L, "冷门店", "/logo.png", new BigDecimal("4.8"), 2800);
        Merchant popular = merchant(10L, "热门店", "/logo.png", new BigDecimal("4.8"), 2800);

        stubFavorites(10L, 15, 11L, 1);

        when(ordersMapper.selectList(any())).thenReturn(
            List.of(),
            ordersForMerchant(11L, 1),
            ordersForMerchant(10L, 12)
        );
        when(reviewMapper.selectCount(any())).thenReturn(1L, 12L);

        List<Merchant> sorted = recommendService.sortByRecommend(List.of(quiet, popular), "13800000001");

        assertEquals(10L, sorted.get(0).getId());
    }

    private void stubFavorites(long firstMerchantId, int firstCount, long secondMerchantId, int secondCount) {
        List<Favorite> favorites = new ArrayList<>();
        IntStream.range(0, firstCount).forEach(i -> favorites.add(favorite(firstMerchantId, "f-" + firstMerchantId + "-" + i)));
        IntStream.range(0, secondCount).forEach(i -> favorites.add(favorite(secondMerchantId, "f-" + secondMerchantId + "-" + i)));
        when(favoriteMapper.selectList(any())).thenReturn(favorites);
    }

    private void stubReviewCounts(List<Long> merchantIds, List<Integer> reviewCounts) {
        when(ordersMapper.selectList(any())).thenReturn(
            ordersForMerchant(merchantIds.get(0), reviewCounts.get(0)),
            ordersForMerchant(merchantIds.get(1), reviewCounts.get(1))
        );
        when(reviewMapper.selectCount(any())).thenReturn(
            reviewCounts.get(0).longValue(),
            reviewCounts.get(1).longValue()
        );
    }

    private List<Orders> ordersForMerchant(long merchantId, int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> {
                Orders order = new Orders();
                order.setId(merchantId * 1000 + i);
                order.setMerchantId(merchantId);
                order.setStatus("COMPLETED");
                return order;
            })
            .toList();
    }

    private Favorite favorite(long merchantId, String userId) {
        Favorite favorite = new Favorite();
        favorite.setMerchantId(merchantId);
        favorite.setUserId(userId);
        return favorite;
    }

    private Merchant merchant(Long id, String name, String logo, BigDecimal score, int averagePrice) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setMerchantName(name);
        merchant.setLogo(logo);
        merchant.setScore(score);
        merchant.setAveragePrice(averagePrice);
        merchant.setStatus(MerchantStatusEnum.OPEN);
        return merchant;
    }
}
