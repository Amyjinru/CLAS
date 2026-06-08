package com.clas.controller;

import com.clas.common.BusinessException;
import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.MerchantAuditRequest;
import com.clas.dto.MerchantRegisterRequest;
import com.clas.dto.MerchantResponse;
import com.clas.dto.OrderResponse;
import com.clas.entity.MerchantAuditLog;
import com.clas.entity.Orders;
import com.clas.service.MerchantService;
import com.clas.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {
    private final MerchantService merchantService;
    private final OrderService orderService;

    public MerchantController(MerchantService merchantService, OrderService orderService) {
        this.merchantService = merchantService;
        this.orderService = orderService;
    }

    @GetMapping("/list")
    public Result<List<MerchantResponse>> list() {
        return Result.ok(merchantService.list());
    }

    @GetMapping("/{id}")
    public Result<MerchantResponse> detail(@PathVariable Long id) {
        return Result.ok(merchantService.detail(id));
    }

    @GetMapping("/order/{merchantId}")
    public Result<List<OrderResponse>> listOrders(@PathVariable Long merchantId) {
        return Result.ok(orderService.listForMerchant(merchantId));
    }

    @PostMapping("/order/accept/{orderId}")
    public Result<Orders> accept(@PathVariable Long orderId) {
        return Result.ok(orderService.accept(orderId));
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
