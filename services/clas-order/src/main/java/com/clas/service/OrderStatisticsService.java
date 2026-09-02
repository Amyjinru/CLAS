package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.CatalogClient;
import com.clas.dto.MerchantStatsDTO;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.entity.Product;
import com.clas.entity.Review;
import com.clas.dto.MerchantSalesRank;
import com.clas.dto.OrderDashboardStats;
import com.clas.dto.OrderStatsDTO;
import com.clas.dto.ProductSalesRank;
import com.clas.dto.SalesOverviewDTO;
import com.clas.mapper.OrderItemMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ReviewMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderStatisticsService {
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final List<String> PURCHASE_STATUSES = List.of("PAID", "ACCEPTED", "DELIVERING", "COMPLETED");

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;
    private final CatalogClient catalogClient;
    private final JdbcTemplate jdbcTemplate;

    public OrderStatisticsService(
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        ReviewMapper reviewMapper,
        CatalogClient catalogClient,
        JdbcTemplate jdbcTemplate
    ) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.reviewMapper = reviewMapper;
        this.catalogClient = catalogClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public OrderDashboardStats getDashboardStats(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);
        LocalDateTime todayStart = range.startDate().atStartOfDay();
        LocalDateTime todayEnd = range.endDate().atTime(LocalTime.MAX);
        return new OrderDashboardStats(
            ordersMapper.selectCount(null),
            queryLong("""
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE status <> 'PENDING_PAYMENT'
                """),
            ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .ge(Orders::getCreateTime, todayStart)
                .le(Orders::getCreateTime, todayEnd)),
            queryLong("""
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE status <> 'PENDING_PAYMENT'
                  AND create_time >= ?
                  AND create_time <= ?
                """, todayStart, todayEnd),
            ordersMapper.selectCount(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, "PENDING_PAYMENT")),
            ordersMapper.selectCount(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, "PAID")),
            ordersMapper.selectCount(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, "COMPLETED"))
        );
    }

    public OrderStatsDTO getOrderStats(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);
        LocalDateTime rangeStart = range.startDate().atStartOfDay();
        LocalDateTime rangeEnd = range.endDate().atTime(LocalTime.MAX);
        List<OrderStatsDTO.StatusCount> statusCounts = jdbcTemplate.query("""
            SELECT COALESCE(status, 'UNKNOWN') AS status, COUNT(*) AS count
            FROM orders
            WHERE create_time >= ? AND create_time <= ?
            GROUP BY status
            """,
            (rs, rowNum) -> new OrderStatsDTO.StatusCount(rs.getString("status"), rs.getLong("count")),
            rangeStart,
            rangeEnd
        );
        List<OrderStatsDTO.DailyCount> dailyOrders = new ArrayList<>();
        for (LocalDate date : daysInRange(range)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            dailyOrders.add(new OrderStatsDTO.DailyCount(
                date.toString(),
                queryLong("SELECT COUNT(*) FROM orders WHERE create_time >= ? AND create_time <= ?", dayStart, dayEnd),
                queryLong("SELECT COALESCE(SUM(total_price), 0) FROM orders WHERE create_time >= ? AND create_time <= ?", dayStart, dayEnd)
            ));
        }
        return new OrderStatsDTO(statusCounts, dailyOrders);
    }

    public SalesOverviewDTO getSalesOverview(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);
        List<SalesOverviewDTO.DailySale> dailySales = new ArrayList<>();
        for (LocalDate date : daysInRange(range)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            dailySales.add(new SalesOverviewDTO.DailySale(
                date.toString(),
                queryLong("""
                    SELECT COALESCE(SUM(total_price), 0)
                    FROM orders
                    WHERE status <> 'PENDING_PAYMENT'
                      AND create_time >= ?
                      AND create_time <= ?
                    """, dayStart, dayEnd),
                queryLong("SELECT COUNT(*) FROM orders WHERE create_time >= ? AND create_time <= ?", dayStart, dayEnd)
            ));
        }
        LocalDate today = LocalDate.now();
        return new SalesOverviewDTO(
            dailySales,
            queryLong("SELECT COALESCE(SUM(total_price), 0) FROM orders WHERE status <> 'PENDING_PAYMENT'"),
            queryLong("""
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE status <> 'PENDING_PAYMENT' AND create_time >= ?
                """, today.withDayOfMonth(1).atStartOfDay()),
            queryLong("""
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE status <> 'PENDING_PAYMENT' AND create_time >= ?
                """, today.minusDays(6).atStartOfDay())
        );
    }

    public List<MerchantSalesRank> getMerchantSalesRanking(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        return jdbcTemplate.query("""
            SELECT merchant_id,
                   COALESCE(SUM(CASE WHEN status <> 'PENDING_PAYMENT' THEN total_price ELSE 0 END), 0) AS total_sales,
                   COUNT(id) AS order_count
            FROM orders
            GROUP BY merchant_id
            ORDER BY total_sales DESC
            LIMIT ?
            """,
            (rs, rowNum) -> new MerchantSalesRank(
                rs.getLong("merchant_id"),
                rs.getLong("total_sales"),
                rs.getLong("order_count")
            ),
            size
        );
    }

    public Map<Long, MerchantSalesRank> getMerchantSales(Collection<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = merchantIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> args = new ArrayList<>(merchantIds);
        List<MerchantSalesRank> ranks = jdbcTemplate.query("""
            SELECT merchant_id,
                   COALESCE(SUM(CASE WHEN status <> 'PENDING_PAYMENT' THEN total_price ELSE 0 END), 0) AS total_sales,
                   COUNT(id) AS order_count
            FROM orders
            WHERE merchant_id IN (""" + placeholders + """
            )
            GROUP BY merchant_id
            """,
            (rs, rowNum) -> new MerchantSalesRank(
                rs.getLong("merchant_id"),
                rs.getLong("total_sales"),
                rs.getLong("order_count")
            ),
            args.toArray()
        );
        Map<Long, MerchantSalesRank> result = new HashMap<>();
        for (MerchantSalesRank rank : ranks) {
            result.put(rank.merchantId(), rank);
        }
        return result;
    }

    public List<ProductSalesRank> getProductSalesRanking(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        return jdbcTemplate.query("""
            SELECT product_id,
                   COALESCE(SUM(quantity), 0) AS sold_count,
                   COALESCE(SUM(price * quantity), 0) AS total_amount
            FROM order_item
            GROUP BY product_id
            ORDER BY sold_count DESC
            LIMIT ?
            """,
            (rs, rowNum) -> new ProductSalesRank(
                rs.getLong("product_id"),
                rs.getLong("sold_count"),
                rs.getLong("total_amount")
            ),
            size
        );
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

    private DateRange resolveRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(6) : startDate;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        if (ChronoUnit.DAYS.between(start, end) > 30) {
            start = end.minusDays(30);
        }
        return new DateRange(start, end);
    }

    private List<LocalDate> daysInRange(DateRange range) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate cursor = range.startDate();
        while (!cursor.isAfter(range.endDate())) {
            days.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private static final class ProductAggregate {
        private long soldCount;
        private long totalAmount;
    }
}
