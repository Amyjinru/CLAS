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
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员后台统一控制器 — 同学E实现
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
    public Result<DashboardStats> dashboard() {
        return Result.ok(statisticsService.getDashboardStats());
    }

    // ==================== 订单统计 ====================

    @GetMapping("/stats/orders")
    public Result<OrderStatsDTO> orderStats() {
        return Result.ok(statisticsService.getOrderStats());
    }

    @GetMapping("/stats/sales")
    public Result<SalesOverviewDTO> salesOverview() {
        return Result.ok(statisticsService.getSalesOverview());
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
        @RequestParam(required = false) String status
    ) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Orders::getStatus, status);
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
        @RequestParam(defaultValue = "10") int size
    ) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
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
        @RequestParam(defaultValue = "10") int size
    ) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Review::getId);

        Page<Review> result = reviewMapper.selectPage(new Page<>(page, size), wrapper);

        // 补充关联信息
        List<Map<String, Object>> enrichedRecords = result.getRecords().stream().map(r -> {
            Orders order = ordersMapper.selectById(r.getOrderId());
            User user = userMapper.selectById(r.getUserId());
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
    @Transactional
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
}
