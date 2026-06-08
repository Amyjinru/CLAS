package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.CreateOrderRequest;
import com.clas.dto.OrderResponse;
import com.clas.dto.PaymentRequest;
import com.clas.dto.PaymentResponse;
import com.clas.entity.Orders;
import com.clas.service.MerchantService;
import com.clas.service.OrderService;
import com.clas.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final MerchantService merchantService;

    public OrderController(OrderService orderService, PaymentService paymentService, MerchantService merchantService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.merchantService = merchantService;
    }

    @PostMapping("/create")
    @RequireRole("USER")
    public Result<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.create(new CreateOrderRequest(currentUserId(), request.merchantId())));
    }

    @GetMapping("/list/{userId}")
    @RequireRole("USER")
    public Result<List<OrderResponse>> list(@PathVariable String userId) {
        return Result.ok(orderService.listForUser(currentUserId()));
    }

    @GetMapping("/merchant/{merchantId}")
    @RequireRole("MERCHANT")
    public Result<List<OrderResponse>> merchantOrders(@PathVariable Long merchantId) {
        return Result.ok(orderService.listForMerchant(merchantService.getCurrentMerchantId()));
    }

    @PostMapping("/pay/{orderId}")
    @RequireRole("USER")
    public Result<PaymentResponse> pay(@PathVariable Long orderId) {
        Orders order = orderService.requireOrder(orderId);
        PaymentRequest request = new PaymentRequest(orderId, currentUserId(), "MOCK");
        return Result.ok(paymentService.mockPay(request));
    }

    @PostMapping("/accept/{orderId}")
    @RequireRole("MERCHANT")
    public Result<Orders> accept(@PathVariable Long orderId) {
        return Result.ok(orderService.accept(orderId, merchantService.getCurrentMerchantId()));
    }

    @PostMapping("/complete/{orderId}")
    @RequireRole("USER")
    public Result<Orders> complete(@PathVariable Long orderId) {
        return Result.ok(orderService.complete(orderId, currentUserId()));
    }

    @PostMapping("/cancel/{orderId}")
    @RequireRole("USER")
    public Result<Orders> cancel(@PathVariable Long orderId) {
        return Result.ok(orderService.cancel(orderId, currentUserId()));
    }

    @PostMapping("/reject/{orderId}")
    @RequireRole("MERCHANT")
    public Result<Orders> reject(@PathVariable Long orderId) {
        return Result.ok(orderService.reject(orderId, merchantService.getCurrentMerchantId()));
    }

    @PostMapping("/refund/{orderId}")
    @RequireRole("USER")
    public Result<Orders> refund(@PathVariable Long orderId) {
        return Result.ok(orderService.refund(orderId, currentUserId()));
    }

    private String currentUserId() {
        return UserContext.getUserId();
    }
}
