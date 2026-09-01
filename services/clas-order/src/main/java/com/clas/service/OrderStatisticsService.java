package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.CatalogClient;
import com.clas.dto.MerchantStatsDTO;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.entity.Product;
import com.clas.entity.Review;
import com.clas.mapper.OrderItemMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ReviewMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderStatisticsService {
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final List<String> PURCHASE_STATUSES = List.of("PAID", "ACCEPTED", "DELIVERING", "COMPLETED");

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;
    private final CatalogClient catalogClient;

    public OrderStatisticsService(
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        ReviewMapper reviewMapper,
        CatalogClient catalogClient
    ) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.reviewMapper = reviewMapper;
        this.catalogClient = catalogClient;
    }

    public Map<Long, CompletedOrderStats> getCompletedOrderStats(Collection<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        List<Orders> completedOrders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .in(Orders::getMerchantId, merchantIds)
            .eq(Orders::getStatus, STATUS_COMPLETED));
        Map<Long, CompletedOrderStats> stats = new HashMap<>();
        for (Orders order : completedOrders) {
            CompletedOrderStats current = stats.getOrDefault(order.getMerchantId(), CompletedOrderStats.EMPTY);
            stats.put(order.getMerchantId(), current.add(order.getTotalPrice()));
        }
        return stats;
    }

    public Map<Long, Integer> getUserPurchaseCounts(String userId) {
        if (userId == null || userId.isBlank()) {
            return Map.of();
        }
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getUserId, userId)
            .in(Orders::getStatus, PURCHASE_STATUSES));
        Map<Long, Integer> counts = new HashMap<>();
        for (Orders order : orders) {
            counts.merge(order.getMerchantId(), 1, Integer::sum);
        }
        return counts;
    }

    public Map<Long, Long> getReviewCounts(Collection<Long> merchantIds) {
        Map<Long, Long> counts = new HashMap<>();
        for (Long merchantId : merchantIds) {
            List<Long> orderIds = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getMerchantId, merchantId))
                .stream()
                .map(Orders::getId)
                .toList();
            if (orderIds.isEmpty()) {
                counts.put(merchantId, 0L);
                continue;
            }
            long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().in(Review::getOrderId, orderIds));
            counts.put(merchantId, count);
        }
        return counts;
    }

    public MerchantStatsDTO getMerchantStats(Long merchantId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        long todayOrders = countOrders(merchantId, todayStart, todayEnd);
        long todaySales = sumSales(merchantId, todayStart, todayEnd);
        long monthSales = sumSales(merchantId, today.withDayOfMonth(1).atStartOfDay(), todayEnd);
        long yearSales = sumSales(merchantId, today.withDayOfYear(1).atStartOfDay(), todayEnd);
        long totalSales = sumSales(merchantId, null, null);

        List<MerchantStatsDTO.DailySale> dailySales = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            dailySales.add(new MerchantStatsDTO.DailySale(
                date.toString(),
                countOrders(merchantId, dayStart, dayEnd),
                sumSales(merchantId, dayStart, dayEnd)
            ));
        }

        List<MerchantStatsDTO.ProductRank> topProducts = topProducts(merchantId);
        return new MerchantStatsDTO(todayOrders, todaySales, monthSales, yearSales, totalSales, dailySales, topProducts);
    }

    private long countOrders(Long merchantId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Orders> wrapper = basePaidWrapper(merchantId);
        if (start != null) {
            wrapper.ge(Orders::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(Orders::getCreateTime, end);
        }
        return ordersMapper.selectCount(wrapper);
    }

    private long sumSales(Long merchantId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Orders> wrapper = basePaidWrapper(merchantId);
        if (start != null) {
            wrapper.ge(Orders::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(Orders::getCreateTime, end);
        }
        List<Orders> orders = ordersMapper.selectList(wrapper);
        return orders.stream().mapToLong(order -> order.getTotalPrice() == null ? 0 : order.getTotalPrice()).sum();
    }

    private LambdaQueryWrapper<Orders> basePaidWrapper(Long merchantId) {
        return new LambdaQueryWrapper<Orders>()
            .eq(Orders::getMerchantId, merchantId)
            .ne(Orders::getStatus, "PENDING_PAYMENT");
    }

    private List<MerchantStatsDTO.ProductRank> topProducts(Long merchantId) {
        List<Long> orderIds = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getMerchantId, merchantId)
                .ne(Orders::getStatus, "PENDING_PAYMENT"))
            .stream()
            .map(Orders::getId)
            .toList();
        if (orderIds.isEmpty()) {
            return List.of();
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
            .in(OrderItem::getOrderId, orderIds));
        Map<Long, ProductAggregate> aggregates = new HashMap<>();
        for (OrderItem item : items) {
            ProductAggregate aggregate = aggregates.computeIfAbsent(item.getProductId(), key -> new ProductAggregate());
            aggregate.soldCount += item.getQuantity() == null ? 0 : item.getQuantity();
            int price = item.getPrice() == null ? 0 : item.getPrice();
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            aggregate.totalAmount += (long) price * quantity;
        }
        Map<Long, Product> products = catalogClient.getProducts(aggregates.keySet());
        return aggregates.entrySet().stream()
            .sorted(Comparator.comparingLong((Map.Entry<Long, ProductAggregate> entry) -> entry.getValue().soldCount).reversed())
            .limit(10)
            .map(entry -> {
                Product product = products.get(entry.getKey());
                String name = product == null ? "商品#" + entry.getKey() : product.getName();
                return new MerchantStatsDTO.ProductRank(
                    entry.getKey(),
                    name,
                    entry.getValue().soldCount,
                    entry.getValue().totalAmount
                );
            })
            .toList();
    }

    public record CompletedOrderStats(long count, long totalPrice) {
        public static final CompletedOrderStats EMPTY = new CompletedOrderStats(0, 0);

        public CompletedOrderStats add(Integer totalPrice) {
            int price = totalPrice == null ? 0 : totalPrice;
            return new CompletedOrderStats(count + 1, this.totalPrice + price);
        }

        public int averagePrice() {
            if (count <= 0) {
                return 0;
            }
            return (int) Math.round((double) totalPrice / count);
        }
    }

    private static final class ProductAggregate {
        private long soldCount;
        private long totalAmount;
    }
}
