package com.clas.controller;

import com.clas.client.IamClient;
import com.clas.client.OrderClient;
import com.clas.common.BusinessException;
import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.AdminReviewRecord;
import com.clas.dto.DashboardStats;
import com.clas.dto.InternalPage;
import com.clas.dto.InternalUserProfile;
import com.clas.dto.MerchantRankingDTO;
import com.clas.dto.OrderStatsDTO;
import com.clas.dto.PenaltyRequest;
import com.clas.dto.RefundDisputeAuditRequest;
import com.clas.dto.SalesOverviewDTO;
import com.clas.dto.TopProductDTO;
import com.clas.entity.Appeal;
import com.clas.entity.DeletedReviewBackup;
import com.clas.entity.OrderRefundDispute;
import com.clas.entity.Orders;
import com.clas.entity.Review;
import com.clas.entity.ReviewDeleteRequest;
import com.clas.entity.UserPenalty;
import com.clas.service.AppealService;
import com.clas.service.PenaltyService;
import com.clas.service.StatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminController {
    private final StatisticsService statisticsService;
    private final IamClient iamClient;
    private final OrderClient orderClient;
    private final PenaltyService penaltyService;
    private final AppealService appealService;

    public AdminController(
        StatisticsService statisticsService,
        IamClient iamClient,
        OrderClient orderClient,
        PenaltyService penaltyService,
        AppealService appealService
    ) {
        this.statisticsService = statisticsService;
        this.iamClient = iamClient;
        this.orderClient = orderClient;
        this.penaltyService = penaltyService;
        this.appealService = appealService;
    }

    private BusinessException orderDomainUnavailable() {
        return new BusinessException(503, "评价/退款争议管理请通过 order 服务处理");
    }

    @GetMapping("/dashboard")
    public Result<DashboardStats> dashboard(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(statisticsService.getDashboardStats(startDate, endDate));
    }

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

    @GetMapping("/order-refund-disputes")
    public Result<List<OrderRefundDispute>> orderRefundDisputes(@RequestParam(required = false) String status) {
        throw orderDomainUnavailable();
    }

    @PatchMapping("/order-refund-disputes/{id}")
    public Result<OrderRefundDispute> auditOrderRefundDispute(
        @PathVariable Long id, @Valid @RequestBody RefundDisputeAuditRequest request
    ) {
        throw orderDomainUnavailable();
    }

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
        return Result.ok(toPage(orderClient.listAdminOrders(page, size, status, deliveryStatus, startDate, endDate, keyword)));
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false) String keyword
    ) {
        return Result.ok(toPage(iamClient.listUsers(page, size, role, enabled, keyword)));
    }

    @PutMapping("/users/{phone}/status")
    public Result<InternalUserProfile> toggleUserStatus(@PathVariable String phone, @RequestBody Map<String, Boolean> body) {
        if (!body.getOrDefault("enabled", true)) {
            throw new BusinessException("请使用账户封禁处罚接口，并提供原因和时长");
        }
        return Result.ok(iamClient.restoreAccount(phone, UserContext.getUserId()));
    }

    @GetMapping("/reviews")
    public Result<Map<String, Object>> listReviews(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String reportStatus,
        @RequestParam(required = false) String keyword
    ) {
        InternalPage<AdminReviewRecord> result = orderClient.listAdminReviews(page, size, reportStatus, keyword);
        Map<String, InternalUserProfile> users = iamClient.getUserProfiles(
            result.records().stream().map(AdminReviewRecord::userId).distinct().toList()
        );
        List<Map<String, Object>> enriched = result.records().stream().map(review -> {
            InternalUserProfile user = users.get(review.userId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", review.id());
            map.put("orderId", review.orderId());
            map.put("userId", review.userId());
            map.put("username", user != null && user.username() != null ? user.username() : "未知");
            map.put("score", review.score());
            map.put("content", review.content());
            map.put("merchantReply", review.merchantReply());
            map.put("reportReason", review.reportReason());
            map.put("reportStatus", review.reportStatus());
            map.put("merchantId", review.merchantId());
            return map;
        }).toList();
        return Result.ok(Map.of(
            "records", enriched,
            "total", result.total(),
            "page", result.page(),
            "size", result.size()
        ));
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        throw orderDomainUnavailable();
    }

    @PutMapping("/reviews/{id}/report-status")
    public Result<Review> resolveReviewReport(@PathVariable Long id, @RequestBody Map<String, String> body) {
        throw orderDomainUnavailable();
    }

    @GetMapping("/reviews/deleted-backups")
    public Result<List<DeletedReviewBackup>> deletedBackups() {
        throw orderDomainUnavailable();
    }

    @GetMapping("/reviews/delete-requests")
    public Result<List<ReviewDeleteRequest>> deleteRequests(@RequestParam(required = false) String status) {
        throw orderDomainUnavailable();
    }

    @PostMapping("/reviews/delete-requests/{id}/process")
    public Result<Void> processDeleteRequest(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        throw orderDomainUnavailable();
    }

    @PostMapping("/users/{phone}/penalties")
    public Result<UserPenalty> applyPenalty(@PathVariable String phone, @Valid @RequestBody PenaltyRequest request) {
        PenaltyRequest payload = new PenaltyRequest(phone, request.penaltyType(), request.reason(), request.durationHours());
        return Result.ok(penaltyService.applyPenalty(payload, UserContext.getUserId()));
    }

    @PostMapping("/users/{phone}/account-ban/restore")
    public Result<Void> restoreAccountBan(@PathVariable String phone) {
        penaltyService.restoreAccount(phone, UserContext.getUserId());
        return Result.ok();
    }

    @PostMapping("/penalties/{id}/revoke")
    public Result<Void> revokePenalty(@PathVariable Long id) {
        penaltyService.revokePenalty(id, UserContext.getUserId());
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
            UserContext.getUserId()
        ));
    }

    @GetMapping("/export/users")
    public void exportUsers(
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false) String keyword,
        HttpServletResponse response
    ) throws IOException {
        List<InternalUserProfile> users = iamClient.exportUsers(role, enabled, keyword);
        setCsvHeaders(response, "users.csv");
        PrintWriter w = response.getWriter();
        w.println("﻿手机号,用户名,角色,状态,昵称");
        for (InternalUserProfile user : users) {
            w.println(String.join(",",
                csvEscape(user.phone()),
                csvEscape(user.username()),
                csvEscape(user.role()),
                Boolean.TRUE.equals(user.enabled()) ? "启用" : "禁用",
                csvEscape(user.nickname() != null ? user.nickname() : "")
            ));
        }
        w.flush();
    }

    @GetMapping("/export/orders")
    public void exportOrders(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String deliveryStatus,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String keyword,
        HttpServletResponse response
    ) throws IOException {
        List<Orders> orders = orderClient.exportOrders(status, deliveryStatus, startDate, endDate, keyword);
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
        List<AdminReviewRecord> reviews = orderClient.exportReviews(reportStatus, keyword);
        setCsvHeaders(response, "reviews.csv");
        PrintWriter w = response.getWriter();
        w.println("﻿评价ID,订单ID,用户手机,评分,内容,举报原因,举报状态,创建时间");
        for (AdminReviewRecord r : reviews) {
            w.println(String.join(",",
                String.valueOf(r.id()),
                String.valueOf(r.orderId()),
                csvEscape(r.userId()),
                String.valueOf(r.score()),
                csvEscape(r.content() != null ? r.content() : ""),
                csvEscape(r.reportReason() != null ? r.reportReason() : ""),
                csvEscape(r.reportStatus()),
                csvEscape(r.createdAt() != null ? r.createdAt().toString() : "")
            ));
        }
        w.flush();
    }

    private Map<String, Object> toPage(InternalPage<?> page) {
        return Map.of(
            "records", page.records(),
            "total", page.total(),
            "page", page.page(),
            "size", page.size()
        );
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
