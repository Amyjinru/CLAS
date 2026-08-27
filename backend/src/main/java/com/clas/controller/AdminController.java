package com.clas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clas.common.BusinessException;
import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.*;
import com.clas.entity.*;
import com.clas.mapper.*;
import com.clas.entity.Appeal;
import com.clas.entity.DeletedReviewBackup;
import com.clas.entity.ReviewDeleteRequest;
import com.clas.entity.UserPenalty;
import com.clas.service.AppealService;
import com.clas.service.PenaltyService;
import com.clas.service.ReviewService;
import com.clas.service.StatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 管理员后台统一控制器
 * 所有接口均需 ADMIN 角色
 */
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminController {

    private final StatisticsService statisticsService;
    private final UserMapper userMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantMapper merchantMapper;
    private final ReviewMapper reviewMapper;
    private final ReviewService reviewService;
    private final ProductMapper productMapper;
    private final PenaltyService penaltyService;
    private final AppealService appealService;

    public AdminController(
        StatisticsService statisticsService,
        UserMapper userMapper,
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        MerchantMapper merchantMapper,
        ReviewMapper reviewMapper,
        ReviewService reviewService,
        ProductMapper productMapper,
        PenaltyService penaltyService,
        AppealService appealService
    ) {
        this.statisticsService = statisticsService;
        this.userMapper = userMapper;
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.merchantMapper = merchantMapper;
        this.reviewMapper = reviewMapper;
        this.reviewService = reviewService;
        this.productMapper = productMapper;
        this.penaltyService = penaltyService;
        this.appealService = appealService;
    }

    // ==================== 仪表盘 ====================

    @GetMapping("/dashboard")
    public Result<DashboardStats> dashboard(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(statisticsService.getDashboardStats(startDate, endDate));
    }

    // ==================== 订单统计 ====================

    @GetMapping("/stats/orders")
    public Result<OrderStatsDTO> orderStats(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(statisticsService.getOrderStats(startDate, endDate));
    }

    @GetMapping("/stats/sales")
    public Result<SalesOverviewDTO> salesOverview(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(statisticsService.getSalesOverview(startDate, endDate));
    }

    @GetMapping("/stats/merchants")
    public Result<MerchantRankingDTO> merchantRanking() {
        return Result.ok(statisticsService.getMerchantRanking());
    }

    @GetMapping("/stats/products")
    public Result<TopProductDTO> topProducts() {
        return Result.ok(statisticsService.getTopProducts());
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    public Result<Map<String, Object>> listOrders(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String deliveryStatus,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Orders::getStatus, status);
        }
        if (deliveryStatus != null && !deliveryStatus.isBlank()) {
            wrapper.eq(Orders::getDeliveryStatus, deliveryStatus);
        }
        if (startDate != null) {
            wrapper.ge(Orders::getCreateTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(Orders::getCreateTime, endDate.atTime(LocalTime.MAX));
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(w -> {
                w.like(Orders::getUserId, normalizedKeyword)
                    .or()
                    .like(Orders::getDeliveryAddress, normalizedKeyword);
                try {
                    Long numeric = Long.valueOf(normalizedKeyword);
                    w.or().eq(Orders::getId, numeric).or().eq(Orders::getMerchantId, numeric);
                } catch (NumberFormatException ignored) {
                    // Non-numeric keywords only search text fields.
                }
            });
        }
        wrapper.orderByDesc(Orders::getCreateTime);

        Page<Orders> result = ordersMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.ok(Map.of(
            "records", result.getRecords(),
            "total", result.getTotal(),
            "page", result.getCurrent(),
            "size", result.getSize()
        ));
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role);
        }
        if (enabled != null) {
            wrapper.eq(User::getEnabled, enabled);
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(w -> w.like(User::getPhone, normalizedKeyword)
                .or()
                .like(User::getUsername, normalizedKeyword)
                .or()
                .like(User::getNickname, normalizedKeyword));
        }
        wrapper.orderByAsc(User::getPhone);
        wrapper.select(User.class, info -> !"password".equals(info.getColumn()));

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.ok(Map.of(
            "records", result.getRecords(),
            "total", result.getTotal(),
            "page", result.getCurrent(),
            "size", result.getSize()
        ));
    }

    @PutMapping("/users/{phone}/status")
    public Result<User> toggleUserStatus(@PathVariable String phone, @RequestBody Map<String, Boolean> body) {
        User user = userMapper.selectById(phone);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setEnabled(body.getOrDefault("enabled", true));
        userMapper.updateById(user);
        user.setPassword(null);
        return Result.ok(user);
    }

    // ==================== 评价管理 ====================

    @GetMapping("/reviews")
    public Result<Map<String, Object>> listReviews(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String reportStatus,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (reportStatus != null && !reportStatus.isBlank()) {
            wrapper.eq(Review::getReportStatus, reportStatus);
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(w -> w.like(Review::getUserId, normalizedKeyword)
                .or()
                .like(Review::getContent, normalizedKeyword)
                .or()
                .like(Review::getReportReason, normalizedKeyword));
        }
        wrapper.orderByDesc(Review::getId);

        Page<Review> result = reviewMapper.selectPage(new Page<>(page, size), wrapper);

        // 补充关联信息
        // 批量预加载关联数据，消除 N+1
        java.util.Set<Long> orderIds = result.getRecords().stream().map(Review::getOrderId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> userIds = result.getRecords().stream().map(Review::getUserId).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, Orders> orderMap = ordersMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Orders>().in(Orders::getId, orderIds))
            .stream().collect(java.util.stream.Collectors.toMap(Orders::getId, o -> o));
        java.util.Map<String, User> userMap = userMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>().in(User::getPhone, userIds))
            .stream().collect(java.util.stream.Collectors.toMap(User::getPhone, u -> u));

        List<Map<String, Object>> enrichedRecords = result.getRecords().stream().map(r -> {
            Orders order = orderMap.get(r.getOrderId());
            User user = userMap.get(r.getUserId());
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", r.getId());
            map.put("orderId", r.getOrderId());
            map.put("userId", r.getUserId());
            map.put("username", user != null ? user.getUsername() : "未知");
            map.put("score", r.getScore());
            map.put("content", r.getContent());
            map.put("merchantReply", r.getMerchantReply());
            map.put("reportReason", r.getReportReason());
            map.put("reportStatus", r.getReportStatus());
            map.put("merchantId", order != null ? order.getMerchantId() : null);
            return map;
        }).toList();

        return Result.ok(Map.of(
            "records", enrichedRecords,
            "total", result.getTotal(),
            "page", result.getCurrent(),
            "size", result.getSize()
        ));
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        reviewService.adminDeleteReview(id);
        return Result.ok();
    }

    @PutMapping("/reviews/{id}/report-status")
    public Result<Review> resolveReviewReport(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(reviewService.resolveReport(id, body.getOrDefault("status", "RESOLVED")));
    }

    @GetMapping("/reviews/deleted-backups")
    public Result<List<DeletedReviewBackup>> deletedBackups() {
        return Result.ok(reviewService.listDeletedBackups());
    }

    @GetMapping("/reviews/delete-requests")
    public Result<List<ReviewDeleteRequest>> deleteRequests(@RequestParam(required = false) String status) {
        return Result.ok(reviewService.listDeleteRequests(status));
    }

    @PostMapping("/reviews/delete-requests/{id}/process")
    public Result<Void> processDeleteRequest(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String remarks = body.get("remarks") == null ? null : String.valueOf(body.get("remarks"));
        reviewService.approveDeleteRequest(id, com.clas.config.UserContext.getUserId(), approve, remarks);
        return Result.ok();
    }

    @PostMapping("/users/{phone}/penalties")
    public Result<UserPenalty> applyPenalty(@PathVariable String phone, @Valid @RequestBody PenaltyRequest request) {
        PenaltyRequest payload = new PenaltyRequest(phone, request.penaltyType(), request.reason(), request.durationHours());
        return Result.ok(penaltyService.applyPenalty(payload, com.clas.config.UserContext.getUserId()));
    }

    @PostMapping("/penalties/{id}/revoke")
    public Result<Void> revokePenalty(@PathVariable Long id) {
        penaltyService.revokePenalty(id, com.clas.config.UserContext.getUserId());
        return Result.ok();
    }

    @GetMapping("/appeals")
    public Result<List<Appeal>> listAppeals() {
        return Result.ok(appealService.listPending());
    }

    @PostMapping("/appeals/{id}/process")
    public Result<Appeal> processAppeal(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(appealService.process(
            id,
            body.getOrDefault("status", "APPROVED"),
            body.get("adminReply"),
            com.clas.config.UserContext.getUserId()
        ));
    }

    // ==================== CSV 导出 ====================

    @GetMapping("/export/users")
    public void exportUsers(
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false) String keyword,
        HttpServletResponse response
    ) throws IOException {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isBlank()) wrapper.eq(User::getRole, role);
        if (enabled != null) wrapper.eq(User::getEnabled, enabled);
        if (keyword != null && !keyword.isBlank()) {
            String nk = keyword.trim();
            wrapper.and(w -> w.like(User::getPhone, nk).or().like(User::getUsername, nk).or().like(User::getNickname, nk));
        }
        wrapper.orderByAsc(User::getPhone);
        List<User> users = userMapper.selectList(wrapper);

        setCsvHeaders(response, "users.csv");
        PrintWriter w = response.getWriter();
        w.println("﻿手机号,用户名,角色,状态,昵称");
        for (User u : users) {
            w.println(String.join(",",
                csvEscape(u.getPhone()),
                csvEscape(u.getUsername()),
                csvEscape(u.getRole()),
                Boolean.TRUE.equals(u.getEnabled()) ? "启用" : "禁用",
                csvEscape(u.getNickname() != null ? u.getNickname() : "")
            ));
        }
        w.flush();
    }

    @GetMapping("/export/orders")
    public void exportOrders(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String keyword,
        HttpServletResponse response
    ) throws IOException {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) wrapper.eq(Orders::getStatus, status);
        if (startDate != null) wrapper.ge(Orders::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) wrapper.le(Orders::getCreateTime, endDate.atTime(LocalTime.MAX));
        if (keyword != null && !keyword.isBlank()) {
            String nk = keyword.trim();
            wrapper.and(w -> {
                w.like(Orders::getUserId, nk).or().like(Orders::getDeliveryAddress, nk);
                try { w.or().eq(Orders::getId, Long.valueOf(nk)).or().eq(Orders::getMerchantId, Long.valueOf(nk)); }
                catch (NumberFormatException ignored) {}
            });
        }
        wrapper.orderByDesc(Orders::getCreateTime);
        List<Orders> orders = ordersMapper.selectList(wrapper);

        setCsvHeaders(response, "orders.csv");
        PrintWriter w = response.getWriter();
        w.println("﻿订单ID,用户手机,商家ID,金额(分),状态,配送状态,退款状态,创建时间,收货地址");
        for (Orders o : orders) {
            w.println(String.join(",",
                String.valueOf(o.getId()),
                csvEscape(o.getUserId()),
                String.valueOf(o.getMerchantId()),
                String.valueOf(o.getTotalPrice()),
                csvEscape(o.getStatus()),
                csvEscape(o.getDeliveryStatus()),
                csvEscape(o.getRefundStatus()),
                csvEscape(o.getCreateTime() != null ? o.getCreateTime().toString() : ""),
                csvEscape(o.getDeliveryAddress() != null ? o.getDeliveryAddress() : "")
            ));
        }
        w.flush();
    }

    @GetMapping("/export/reviews")
    public void exportReviews(
        @RequestParam(required = false) String reportStatus,
        @RequestParam(required = false) String keyword,
        HttpServletResponse response
    ) throws IOException {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (reportStatus != null && !reportStatus.isBlank()) wrapper.eq(Review::getReportStatus, reportStatus);
        if (keyword != null && !keyword.isBlank()) {
            String nk = keyword.trim();
            wrapper.and(w -> w.like(Review::getUserId, nk).or().like(Review::getContent, nk).or().like(Review::getReportReason, nk));
        }
        wrapper.orderByDesc(Review::getId);
        List<Review> reviews = reviewMapper.selectList(wrapper);

        setCsvHeaders(response, "reviews.csv");
        PrintWriter w = response.getWriter();
        w.println("﻿评价ID,订单ID,用户手机,评分,内容,举报原因,举报状态,创建时间");
        for (Review r : reviews) {
            w.println(String.join(",",
                String.valueOf(r.getId()),
                String.valueOf(r.getOrderId()),
                csvEscape(r.getUserId()),
                String.valueOf(r.getScore()),
                csvEscape(r.getContent() != null ? r.getContent() : ""),
                csvEscape(r.getReportReason() != null ? r.getReportReason() : ""),
                csvEscape(r.getReportStatus()),
                csvEscape(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "")
            ));
        }
        w.flush();
    }

    private void setCsvHeaders(HttpServletResponse response, String filename) {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
