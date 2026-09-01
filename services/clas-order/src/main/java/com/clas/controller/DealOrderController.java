package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.DealRedeemLogResponse;
import com.clas.dto.PaymentResponse;
import com.clas.entity.DealOrder;
import com.clas.service.DealOrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deals")
public class DealOrderController {
    private final DealOrderService dealOrderService;

    public DealOrderController(DealOrderService dealOrderService) {
        this.dealOrderService = dealOrderService;
    }

    @GetMapping("/orders/{dealOrderId}/payment-status")
    @RequireRole("USER")
    public Result<PaymentResponse> paymentStatus(@PathVariable Long dealOrderId) {
        return Result.ok(dealOrderService.getDealPaymentStatus(dealOrderId, UserContext.getUserId()));
    }

    @PostMapping("/orders/{dealOrderId}/pay")
    @RequireRole("USER")
    public Result<PaymentResponse> pay(@PathVariable Long dealOrderId, @RequestBody(required = false) Map<String, String> body) {
        String payMethod = body == null ? null : body.get("payMethod");
        return Result.ok(dealOrderService.payDealOrder(dealOrderId, UserContext.getUserId(), payMethod));
    }

    @PostMapping("/{dealId}/buy")
    @RequireRole("USER")
    public Result<DealOrder> buy(@PathVariable Long dealId) {
        return Result.ok(dealOrderService.buy(dealId));
    }

    @GetMapping("/mine")
    @RequireRole("USER")
    public Result<List<DealOrder>> myOrders() {
        return Result.ok(dealOrderService.myOrders());
    }

    @PostMapping("/redeem")
    @RequireRole("MERCHANT")
    public Result<DealOrder> redeem(@RequestBody Map<String, String> body) {
        return Result.ok(dealOrderService.redeem(body.get("voucherCode")));
    }

    @PostMapping("/orders/{dealOrderId}/refund")
    @RequireRole("USER")
    public Result<DealOrder> refund(@PathVariable Long dealOrderId) {
        return Result.ok(dealOrderService.refundDealOrder(dealOrderId, UserContext.getUserId()));
    }

    @GetMapping("/redeem-logs")
    @RequireRole("MERCHANT")
    public Result<List<DealRedeemLogResponse>> redeemLogs() {
        return Result.ok(dealOrderService.redeemLogs());
    }
}
