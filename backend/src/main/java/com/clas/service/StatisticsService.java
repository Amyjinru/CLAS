package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.dto.*;
import com.clas.entity.*;
import com.clas.mapper.*;
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

    public StatisticsService(
        UserMapper userMapper,
        MerchantMapper merchantMapper,
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        PaymentMapper paymentMapper,
        ProductMapper productMapper,
        ReviewMapper reviewMapper
    ) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
        this.productMapper = productMapper;
        this.reviewMapper = reviewMapper;
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

        // 总销售额：所有已支付/已接单/已完成订单的金额总和
        Long totalSales = 0L;
        List<Orders> allOrders = ordersMapper.selectList(null);
        for (Orders o : allOrders) {
            if (!"PENDING_PAYMENT".equals(o.getStatus())) {
                totalSales += o.getTotalPrice() != null ? o.getTotalPrice() : 0;
            }
        }

        // 今日数据
        DateRange range = resolveRange(startDate, endDate);
        LocalDateTime todayStart = range.startDate().atStartOfDay();
        LocalDateTime todayEnd = range.endDate().atTime(LocalTime.MAX);

        Long todayOrders = ordersMapper.selectCount(
            new LambdaQueryWrapper<Orders>()
                .ge(Orders::getCreateTime, todayStart)
                .le(Orders::getCreateTime, todayEnd)
        );

        Long todaySales = 0L;
        List<Orders> todayOrderList = ordersMapper.selectList(
            new LambdaQueryWrapper<Orders>()
                .ge(Orders::getCreateTime, todayStart)
                .le(Orders::getCreateTime, todayEnd)
        );
        for (Orders o : todayOrderList) {
            if (!"PENDING_PAYMENT".equals(o.getStatus())) {
                todaySales += o.getTotalPrice() != null ? o.getTotalPrice() : 0;
            }
        }

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
        List<Orders> allOrders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .ge(Orders::getCreateTime, range.startDate().atStartOfDay())
            .le(Orders::getCreateTime, range.endDate().atTime(LocalTime.MAX)));
        Map<String, Long> statusMap = allOrders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getStatus() != null ? o.getStatus() : "UNKNOWN",
                Collectors.counting()
            ));

        List<OrderStatsDTO.StatusCount> statusCounts = statusMap.entrySet().stream()
            .map(e -> new OrderStatsDTO.StatusCount(e.getKey(), e.getValue()))
            .toList();

        // 近7天每日订单数
        List<OrderStatsDTO.DailyCount> dailyOrders = new ArrayList<>();
        for (LocalDate date : daysInRange(range)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            List<Orders> dayOrders = allOrders.stream()
                .filter(o -> o.getCreateTime() != null)
                .filter(o -> !o.getCreateTime().isBefore(dayStart) && !o.getCreateTime().isAfter(dayEnd))
                .toList();
            long count = dayOrders.size();
            long amount = dayOrders.stream()
                .mapToLong(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
                .sum();

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

            List<Orders> dayOrders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                    .ge(Orders::getCreateTime, dayStart)
                    .le(Orders::getCreateTime, dayEnd)
            );
            long amount = dayOrders.stream()
                .filter(o -> !"PENDING_PAYMENT".equals(o.getStatus()))
                .mapToLong(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
                .sum();
            long orderCount = dayOrders.size();

            dailySales.add(new SalesOverviewDTO.DailySale(date.toString(), amount, orderCount));
            weeklySales += amount;

            if (!date.isBefore(monthStart.toLocalDate())) {
                monthlySales += amount;
            }
        }

        // 总销售额
        List<Orders> allOrders = ordersMapper.selectList(null);
        totalSales = allOrders.stream()
            .filter(o -> !"PENDING_PAYMENT".equals(o.getStatus()))
            .mapToLong(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
            .sum();

        return new SalesOverviewDTO(dailySales, totalSales, monthlySales, weeklySales);
    }

    public SalesOverviewDTO getSalesOverview(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);
        List<SalesOverviewDTO.DailySale> dailySales = new ArrayList<>();
        List<Orders> allOrders = ordersMapper.selectList(null);

        for (LocalDate date : daysInRange(range)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            List<Orders> dayOrders = allOrders.stream()
                .filter(order -> order.getCreateTime() != null)
                .filter(order -> !order.getCreateTime().isBefore(dayStart) && !order.getCreateTime().isAfter(dayEnd))
                .toList();
            long amount = dayOrders.stream()
                .filter(order -> !"PENDING_PAYMENT".equals(order.getStatus()))
                .mapToLong(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0)
                .sum();
            dailySales.add(new SalesOverviewDTO.DailySale(date.toString(), amount, (long) dayOrders.size()));
        }

        long totalSales = allOrders.stream()
            .filter(order -> !"PENDING_PAYMENT".equals(order.getStatus()))
            .mapToLong(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0)
            .sum();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        long weeklySales = allOrders.stream()
            .filter(order -> order.getCreateTime() != null)
            .filter(order -> !order.getCreateTime().toLocalDate().isBefore(weekStart))
            .filter(order -> !"PENDING_PAYMENT".equals(order.getStatus()))
            .mapToLong(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0)
            .sum();
        long monthlySales = allOrders.stream()
            .filter(order -> order.getCreateTime() != null)
            .filter(order -> !order.getCreateTime().isBefore(monthStart))
            .filter(order -> !"PENDING_PAYMENT".equals(order.getStatus()))
            .mapToLong(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0)
            .sum();

        return new SalesOverviewDTO(dailySales, totalSales, monthlySales, weeklySales);
    }

    /**
     * 商家排行（按销售额 + 按评分）
     */
    public MerchantRankingDTO getMerchantRanking() {
        List<Merchant> merchants = merchantMapper.selectList(null);
        List<Orders> allOrders = ordersMapper.selectList(null);

        // 按销售额排行
        Map<Long, Long> merchantSales = new HashMap<>();
        Map<Long, Long> merchantOrderCount = new HashMap<>();
        for (Orders o : allOrders) {
            Long mid = o.getMerchantId();
            merchantSales.merge(mid, o.getTotalPrice() != null ? o.getTotalPrice() : 0L, Long::sum);
            merchantOrderCount.merge(mid, 1L, Long::sum);
        }

        List<MerchantRankingDTO.MerchantRank> bySales = merchants.stream()
            .map(m -> new MerchantRankingDTO.MerchantRank(
                m.getId(), m.getMerchantName(), m.getCategory(), m.getScore(),
                merchantSales.getOrDefault(m.getId(), 0L),
                merchantOrderCount.getOrDefault(m.getId(), 0L)
            ))
            .sorted((a, b) -> Long.compare(b.totalSales(), a.totalSales()))
            .limit(10)
            .toList();

        // 按评分排行
        List<MerchantRankingDTO.MerchantRank> byRating = merchants.stream()
            .map(m -> new MerchantRankingDTO.MerchantRank(
                m.getId(), m.getMerchantName(), m.getCategory(), m.getScore(),
                merchantSales.getOrDefault(m.getId(), 0L),
                merchantOrderCount.getOrDefault(m.getId(), 0L)
            ))
            .sorted((a, b) -> {
                BigDecimal sa = a.score() != null ? a.score() : BigDecimal.ZERO;
                BigDecimal sb = b.score() != null ? b.score() : BigDecimal.ZERO;
                return sb.compareTo(sa);
            })
            .limit(10)
            .toList();

        return new MerchantRankingDTO(bySales, byRating);
    }

    /**
     * 热销商品排行
     */
    public TopProductDTO getTopProducts() {
        List<OrderItem> allItems = orderItemMapper.selectList(null);
        List<Product> allProducts = productMapper.selectList(null);
        List<Merchant> allMerchants = merchantMapper.selectList(null);

        Map<Long, String> productNameMap = allProducts.stream()
            .collect(Collectors.toMap(com.clas.entity.Product::getId, com.clas.entity.Product::getName));
        Map<Long, String> merchantNameMap = allMerchants.stream()
            .collect(Collectors.toMap(Merchant::getId, Merchant::getMerchantName));

        // 按商品聚合销量和销售额
        Map<Long, Long> productSold = new HashMap<>();
        Map<Long, Long> productAmount = new HashMap<>();
        for (OrderItem item : allItems) {
            productSold.merge(item.getProductId(), (long) item.getQuantity(), Long::sum);
            productAmount.merge(item.getProductId(),
                (long) item.getPrice() * item.getQuantity(), Long::sum);
        }

        List<TopProductDTO.ProductRank> ranks = productSold.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(10)
            .map(e -> new TopProductDTO.ProductRank(
                e.getKey(),
                productNameMap.getOrDefault(e.getKey(), "未知商品"),
                merchantNameMap.getOrDefault(
                    allProducts.stream()
                        .filter(p -> p.getId().equals(e.getKey()))
                        .findFirst()
                        .map(com.clas.entity.Product::getMerchantId)
                        .orElse(0L),
                    "未知商家"
                ),
                e.getValue(),
                productAmount.getOrDefault(e.getKey(), 0L)
            ))
            .toList();

        return new TopProductDTO(ranks);
    }

    public MerchantStatsDTO getMerchantStats(Long merchantId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        List<Orders> paidOrders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getMerchantId, merchantId)
            .ne(Orders::getStatus, "PENDING_PAYMENT"));

        long todayOrders = paidOrders.stream()
            .filter(order -> order.getCreateTime() != null)
            .filter(order -> !order.getCreateTime().isBefore(todayStart) && !order.getCreateTime().isAfter(todayEnd))
            .count();
        long todaySales = paidOrders.stream()
            .filter(order -> order.getCreateTime() != null)
            .filter(order -> !order.getCreateTime().isBefore(todayStart) && !order.getCreateTime().isAfter(todayEnd))
            .mapToLong(order -> order.getTotalPrice() == null ? 0 : order.getTotalPrice())
            .sum();

        List<MerchantStatsDTO.DailySale> dailySales = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            List<Orders> dayOrders = paidOrders.stream()
                .filter(order -> order.getCreateTime() != null)
                .filter(order -> !order.getCreateTime().isBefore(dayStart) && !order.getCreateTime().isAfter(dayEnd))
                .toList();
            long amount = dayOrders.stream()
                .mapToLong(order -> order.getTotalPrice() == null ? 0 : order.getTotalPrice())
                .sum();
            dailySales.add(new MerchantStatsDTO.DailySale(date.toString(), (long) dayOrders.size(), amount));
        }

        Set<Long> paidOrderIds = paidOrders.stream()
            .map(Orders::getId)
            .collect(Collectors.toSet());
        List<Product> merchantProducts = productMapper.selectList(new LambdaQueryWrapper<Product>()
            .eq(Product::getMerchantId, merchantId));
        Map<Long, String> productNames = merchantProducts.stream()
            .collect(Collectors.toMap(Product::getId, Product::getName));
        Set<Long> merchantProductIds = productNames.keySet();

        Map<Long, Long> soldCounts = new HashMap<>();
        Map<Long, Long> amounts = new HashMap<>();
        List<OrderItem> allItems = orderItemMapper.selectList(null);
        for (OrderItem item : allItems) {
            if (!paidOrderIds.contains(item.getOrderId()) || !merchantProductIds.contains(item.getProductId())) {
                continue;
            }
            soldCounts.merge(item.getProductId(), (long) item.getQuantity(), Long::sum);
            amounts.merge(item.getProductId(), (long) item.getPrice() * item.getQuantity(), Long::sum);
        }

        List<MerchantStatsDTO.ProductRank> topProducts = soldCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(10)
            .map(entry -> new MerchantStatsDTO.ProductRank(
                entry.getKey(),
                productNames.getOrDefault(entry.getKey(), "未知商品"),
                entry.getValue(),
                amounts.getOrDefault(entry.getKey(), 0L)
            ))
            .toList();

        return new MerchantStatsDTO(todayOrders, todaySales, dailySales, topProducts);
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
}
