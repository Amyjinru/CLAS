package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.dto.*;
import com.clas.entity.*;
import com.clas.mapper.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计服务 — 管理后台数据统计模块
 */
@Service
public class StatisticsService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final ProductMapper productMapper;
    private final ReviewMapper reviewMapper;
    private final JdbcTemplate jdbcTemplate;

    public StatisticsService(
        UserMapper userMapper,
        MerchantMapper merchantMapper,
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        PaymentMapper paymentMapper,
        ProductMapper productMapper,
        ReviewMapper reviewMapper,
        JdbcTemplate jdbcTemplate
    ) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
        this.productMapper = productMapper;
        this.reviewMapper = reviewMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 仪表盘汇总数据
     */
    public DashboardStats getDashboardStats() {
        return getDashboardStats(null, null);
    }

    public DashboardStats getDashboardStats(LocalDate startDate, LocalDate endDate) {
        Long totalUsers = userMapper.selectCount(null);
        Long totalMerchants = merchantMapper.selectCount(null);
        Long totalOrders = ordersMapper.selectCount(null);

        Long totalSales = queryLong("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM orders
            WHERE status <> 'PENDING_PAYMENT'
            """);

        // 今日数据
        DateRange range = resolveRange(startDate, endDate);
        LocalDateTime todayStart = range.startDate().atStartOfDay();
        LocalDateTime todayEnd = range.endDate().atTime(LocalTime.MAX);

        Long todayOrders = ordersMapper.selectCount(
            new LambdaQueryWrapper<Orders>()
                .ge(Orders::getCreateTime, todayStart)
                .le(Orders::getCreateTime, todayEnd)
        );

        Long todaySales = queryLong("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM orders
            WHERE status <> 'PENDING_PAYMENT'
              AND create_time >= ?
              AND create_time <= ?
            """, todayStart, todayEnd);

        Long pendingPayment = ordersMapper.selectCount(
            new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, "PENDING_PAYMENT"));
        Long paid = ordersMapper.selectCount(
            new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, "PAID"));
        Long completed = ordersMapper.selectCount(
            new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, "COMPLETED"));

        return new DashboardStats(
            totalUsers, totalMerchants, totalOrders, totalSales,
            todayOrders, todaySales, pendingPayment, paid, completed
        );
    }

    /**
     * 订单统计：按状态分布 + 近7天每日订单数
     */
    public OrderStatsDTO getOrderStats() {
        return getOrderStats(null, null);
    }

    public OrderStatsDTO getOrderStats(LocalDate startDate, LocalDate endDate) {
        // 各状态数量
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

        // 近7天每日订单数
        List<OrderStatsDTO.DailyCount> dailyOrders = new ArrayList<>();
        for (LocalDate date : daysInRange(range)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long count = queryLong("""
                SELECT COUNT(*)
                FROM orders
                WHERE create_time >= ? AND create_time <= ?
                """, dayStart, dayEnd);
            long amount = queryLong("""
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE create_time >= ? AND create_time <= ?
                """, dayStart, dayEnd);

            dailyOrders.add(new OrderStatsDTO.DailyCount(date.toString(), count, amount));
        }

        return new OrderStatsDTO(statusCounts, dailyOrders);
    }

    /**
     * 销售额概览
     */
    public SalesOverviewDTO getSalesOverview() {
        List<SalesOverviewDTO.DailySale> dailySales = new ArrayList<>();
        long totalSales = 0;
        long weeklySales = 0;
        long monthlySales = 0;

        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long amount = queryLong("""
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE status <> 'PENDING_PAYMENT'
                  AND create_time >= ?
                  AND create_time <= ?
                """, dayStart, dayEnd);
            long orderCount = queryLong("""
                SELECT COUNT(*)
                FROM orders
                WHERE create_time >= ? AND create_time <= ?
                """, dayStart, dayEnd);

            dailySales.add(new SalesOverviewDTO.DailySale(date.toString(), amount, orderCount));
            weeklySales += amount;

            if (!date.isBefore(monthStart.toLocalDate())) {
                monthlySales += amount;
            }
        }

        // 总销售额
        totalSales = queryLong("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM orders
            WHERE status <> 'PENDING_PAYMENT'
            """);

        return new SalesOverviewDTO(dailySales, totalSales, monthlySales, weeklySales);
    }

    public SalesOverviewDTO getSalesOverview(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);
        List<SalesOverviewDTO.DailySale> dailySales = new ArrayList<>();
        for (LocalDate date : daysInRange(range)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            long amount = queryLong("""
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE status <> 'PENDING_PAYMENT'
                  AND create_time >= ?
                  AND create_time <= ?
                """, dayStart, dayEnd);
            long orderCount = queryLong("""
                SELECT COUNT(*)
                FROM orders
                WHERE create_time >= ? AND create_time <= ?
                """, dayStart, dayEnd);
            dailySales.add(new SalesOverviewDTO.DailySale(date.toString(), amount, orderCount));
        }

        long totalSales = queryLong("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM orders
            WHERE status <> 'PENDING_PAYMENT'
            """);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        long weeklySales = queryLong("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM orders
            WHERE status <> 'PENDING_PAYMENT'
              AND create_time >= ?
            """, weekStart.atStartOfDay());
        long monthlySales = queryLong("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM orders
            WHERE status <> 'PENDING_PAYMENT'
              AND create_time >= ?
            """, monthStart);

        return new SalesOverviewDTO(dailySales, totalSales, monthlySales, weeklySales);
    }

    /**
     * 商家排行（按销售额 + 按评分）
     */
    public MerchantRankingDTO getMerchantRanking() {
        String rankingSql = """
            SELECT m.id AS merchant_id,
                   m.merchant_name,
                   m.category,
                   m.score,
                   COALESCE(SUM(CASE WHEN o.status <> 'PENDING_PAYMENT' THEN o.total_price ELSE 0 END), 0) AS total_sales,
                   COUNT(o.id) AS order_count
            FROM merchant m
            LEFT JOIN orders o ON o.merchant_id = m.id
            GROUP BY m.id, m.merchant_name, m.category, m.score
            """;
        List<MerchantRankingDTO.MerchantRank> bySales = jdbcTemplate.query(
            rankingSql + " ORDER BY total_sales DESC LIMIT 10",
            (rs, rowNum) -> new MerchantRankingDTO.MerchantRank(
                rs.getLong("merchant_id"),
                rs.getString("merchant_name"),
                rs.getString("category"),
                rs.getBigDecimal("score"),
                rs.getLong("total_sales"),
                rs.getLong("order_count")
            )
        );
        List<MerchantRankingDTO.MerchantRank> byRating = jdbcTemplate.query(
            rankingSql + " ORDER BY score DESC LIMIT 10",
            (rs, rowNum) -> new MerchantRankingDTO.MerchantRank(
                rs.getLong("merchant_id"),
                rs.getString("merchant_name"),
                rs.getString("category"),
                rs.getBigDecimal("score"),
                rs.getLong("total_sales"),
                rs.getLong("order_count")
            )
        );

        return new MerchantRankingDTO(bySales, byRating);
    }

    /**
     * 热销商品排行
     */
    public TopProductDTO getTopProducts() {
        List<TopProductDTO.ProductRank> ranks = jdbcTemplate.query("""
            SELECT p.id AS product_id,
                   p.name AS product_name,
                   m.merchant_name,
                   COALESCE(SUM(oi.quantity), 0) AS sold_count,
                   COALESCE(SUM(oi.price * oi.quantity), 0) AS total_amount
            FROM order_item oi
            JOIN product p ON p.id = oi.product_id
            LEFT JOIN merchant m ON m.id = p.merchant_id
            GROUP BY p.id, p.name, m.merchant_name
            ORDER BY sold_count DESC
            LIMIT 10
            """,
            (rs, rowNum) -> new TopProductDTO.ProductRank(
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getString("merchant_name"),
                rs.getLong("sold_count"),
                rs.getLong("total_amount")
            )
        );

        return new TopProductDTO(ranks);
    }

    public MerchantStatsDTO getMerchantStats(Long merchantId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        long todayOrders = queryLong("""
            SELECT COUNT(*)
            FROM orders
            WHERE merchant_id = ?
              AND status <> 'PENDING_PAYMENT'
              AND create_time >= ?
              AND create_time <= ?
            """, merchantId, todayStart, todayEnd);
        long todaySales = queryLong("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM orders
            WHERE merchant_id = ?
              AND status <> 'PENDING_PAYMENT'
              AND create_time >= ?
              AND create_time <= ?
            """, merchantId, todayStart, todayEnd);
        long monthSales = queryMerchantSales(
            merchantId,
            today.withDayOfMonth(1).atStartOfDay(),
            todayEnd
        );
        long yearSales = queryMerchantSales(
            merchantId,
            today.withDayOfYear(1).atStartOfDay(),
            todayEnd
        );
        long totalSales = queryLong("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM orders
            WHERE merchant_id = ?
              AND status <> 'PENDING_PAYMENT'
            """, merchantId);

        List<MerchantStatsDTO.DailySale> dailySales = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            long orderCount = queryLong("""
                SELECT COUNT(*)
                FROM orders
                WHERE merchant_id = ?
                  AND status <> 'PENDING_PAYMENT'
                  AND create_time >= ?
                  AND create_time <= ?
                """, merchantId, dayStart, dayEnd);
            long amount = queryLong("""
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE merchant_id = ?
                  AND status <> 'PENDING_PAYMENT'
                  AND create_time >= ?
                  AND create_time <= ?
                """, merchantId, dayStart, dayEnd);
            dailySales.add(new MerchantStatsDTO.DailySale(date.toString(), orderCount, amount));
        }

        List<MerchantStatsDTO.ProductRank> topProducts = jdbcTemplate.query("""
            SELECT p.id AS product_id,
                   p.name AS product_name,
                   COALESCE(SUM(oi.quantity), 0) AS sold_count,
                   COALESCE(SUM(oi.price * oi.quantity), 0) AS total_amount
            FROM product p
            JOIN order_item oi ON oi.product_id = p.id
            JOIN orders o ON o.id = oi.order_id
            WHERE p.merchant_id = ?
              AND o.status <> 'PENDING_PAYMENT'
            GROUP BY p.id, p.name
            ORDER BY sold_count DESC
            LIMIT 10
            """,
            (rs, rowNum) -> new MerchantStatsDTO.ProductRank(
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getLong("sold_count"),
                rs.getLong("total_amount")
            ),
            merchantId
        );

        return new MerchantStatsDTO(todayOrders, todaySales, monthSales, yearSales, totalSales, dailySales, topProducts);
    }

    private long queryMerchantSales(Long merchantId, LocalDateTime start, LocalDateTime end) {
        return queryLong("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM orders
            WHERE merchant_id = ?
              AND status <> 'PENDING_PAYMENT'
              AND create_time >= ?
              AND create_time <= ?
            """, merchantId, start, end);
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
}
