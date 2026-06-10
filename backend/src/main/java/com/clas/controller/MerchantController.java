package com.clas.controller;

import com.clas.common.BusinessException;
import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.MerchantAuditRequest;
import com.clas.dto.DeliveryEstimateResponse;
import com.clas.dto.MerchantProfileUpdateRequest;
import com.clas.dto.MerchantRegisterRequest;
import com.clas.dto.MerchantResponse;
import com.clas.dto.MerchantStatsDTO;
import com.clas.dto.OrderResponse;
import com.clas.entity.MerchantAuditLog;
import com.clas.entity.Orders;
import com.clas.service.MerchantService;
import com.clas.service.MerchantLogoUploadService;
import com.clas.service.OrderService;
import com.clas.service.StatisticsService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {
    private final MerchantService merchantService;
    private final MerchantLogoUploadService merchantLogoUploadService;
    private final OrderService orderService;
    private final StatisticsService statisticsService;

    public MerchantController(
        MerchantService merchantService,
        MerchantLogoUploadService merchantLogoUploadService,
        OrderService orderService,
        StatisticsService statisticsService
    ) {
        this.merchantService = merchantService;
        this.merchantLogoUploadService = merchantLogoUploadService;
        this.orderService = orderService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/list")
    public Result<List<MerchantResponse>> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) BigDecimal lat,
        @RequestParam(required = false) BigDecimal lng,
        @RequestParam(required = false) Long addressId,
        @RequestParam(required = false) Boolean onlyDeliverable
    ) {
        return Result.ok(merchantService.search(keyword, category, sort, lat, lng, addressId, onlyDeliverable));
    }

    @GetMapping("/{id}")
    public Result<MerchantResponse> detail(@PathVariable Long id) {
        return Result.ok(merchantService.detail(id));
    }

    @GetMapping("/{id}/delivery-estimate")
    public Result<DeliveryEstimateResponse> deliveryEstimate(
        @PathVariable Long id,
        @RequestParam BigDecimal lat,
        @RequestParam BigDecimal lng
    ) {
        return Result.ok(merchantService.deliveryEstimate(id, lat, lng));
    }

    @GetMapping("/order/{merchantId}")
    @RequireRole("MERCHANT")
    public Result<List<OrderResponse>> listOrders(@PathVariable Long merchantId) {
        return Result.ok(orderService.listForMerchant(merchantService.getCurrentMerchantId()));
    }

    @PostMapping("/order/accept/{orderId}")
    @RequireRole("MERCHANT")
    public Result<Orders> accept(@PathVariable Long orderId) {
        return Result.ok(orderService.accept(orderId, merchantService.getCurrentMerchantId()));
    }

    // New merchant registration and status management endpoints

    @PostMapping("/register")
    public Result<MerchantResponse> register(@Valid @RequestBody MerchantRegisterRequest request) {
        String loggedInUserId = UserContext.getUserId();
        return Result.ok(merchantService.register(request, loggedInUserId));
    }

    @GetMapping("/my")
    public Result<MerchantResponse> getMyMerchant() {
        String loggedInUserId = UserContext.getUserId();
        if (loggedInUserId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        MerchantResponse response = merchantService.getMerchantByUserId(loggedInUserId);
        return Result.ok(response);
    }

    @PostMapping("/my/logo")
    @RequireRole("MERCHANT")
    public Result<MerchantResponse> uploadMyLogo(@RequestParam("file") MultipartFile file) {
        return Result.ok(merchantLogoUploadService.uploadAndUpdate(file));
    }

    @PostMapping("/my/profile/send-code")
    @RequireRole("MERCHANT")
    public Result<String> sendProfileUpdateCode(@Valid @RequestBody MerchantProfileUpdateRequest request) {
        merchantService.sendProfileUpdateCode(request);
        return Result.ok("验证码已发送");
    }

    @PutMapping("/my/profile")
    @RequireRole("MERCHANT")
    public Result<MerchantResponse> updateMyProfile(@Valid @RequestBody MerchantProfileUpdateRequest request) {
        return Result.ok(merchantService.updateMyProfile(request));
    }

    @PostMapping("/my/manual-closed/toggle")
    @RequireRole("MERCHANT")
    public Result<MerchantResponse> toggleManualClosed() {
        return Result.ok(merchantService.toggleManualClosed());
    }

    @GetMapping("/my/audit-status")
    @RequireRole("MERCHANT")
    public Result<Map<String, Object>> getMyAuditStatus() {
        String loggedInUserId = UserContext.getUserId();
        if (loggedInUserId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        MerchantResponse merchant = merchantService.getMerchantByUserId(loggedInUserId);
        if (merchant == null) {
            throw new BusinessException("当前用户未入驻为商家");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", merchant.status());
        response.put("adminRemarks", merchant.adminRemarks());
        response.put("auditTimeline", merchantService.getAuditLogs(merchant.id()));
        return Result.ok(response);
    }

    @GetMapping("/my/stats")
    @RequireRole("MERCHANT")
    public Result<MerchantStatsDTO> getMyStats() {
        return Result.ok(statisticsService.getMerchantStats(merchantService.getCurrentMerchantId()));
    }

    @GetMapping("/admin/list")
    @RequireRole("ADMIN")
    public Result<List<MerchantResponse>> listAll() {
        return Result.ok(merchantService.listAll());
    }

    @PostMapping("/admin/audit/{id}")
    @RequireRole("ADMIN")
    public Result<MerchantResponse> audit(@PathVariable Long id, @Valid @RequestBody MerchantAuditRequest request) {
        String adminId = UserContext.getUserId();
        if (adminId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        return Result.ok(merchantService.audit(id, request, adminId));
    }

    @GetMapping("/admin/audit-logs/{id}")
    @RequireRole("ADMIN")
    public Result<List<MerchantAuditLog>> getAuditLogs(@PathVariable Long id) {
        return Result.ok(merchantService.getAuditLogs(id));
    }
}
