package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.CreateOrderRequest;
import com.clas.dto.OrderResponse;
import com.clas.dto.PaymentRequest;
import com.clas.dto.PaymentResponse;
import com.clas.entity.Orders;
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

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public Result<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.create(request));
    }

    @GetMapping("/list/{userId}")
    public Result<List<OrderResponse>> list(@PathVariable String userId) {
        return Result.ok(orderService.listForUser(userId));
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<List<OrderResponse>> merchantOrders(@PathVariable Long merchantId) {
        return Result.ok(orderService.listForMerchant(merchantId));
    }

    @PostMapping("/pay/{orderId}")
    public Result<PaymentResponse> pay(@PathVariable Long orderId) {
        Orders order = orderService.requireOrder(orderId);
        PaymentRequest request = new PaymentRequest(orderId, order.getUserId(), "MOCK");
        return Result.ok(paymentService.mockPay(request));
    }

    @PostMapping("/accept/{orderId}")
    public Result<Orders> accept(@PathVariable Long orderId) {
        return Result.ok(orderService.accept(orderId));
    }

    @PostMapping("/complete/{orderId}")
    public Result<Orders> complete(@PathVariable Long orderId) {
        return Result.ok(orderService.complete(orderId));
    }

    @PostMapping("/cancel/{orderId}")
    public Result<Orders> cancel(@PathVariable Long orderId) {
        return Result.ok(orderService.cancel(orderId));
    }

    @PostMapping("/reject/{orderId}")
    public Result<Orders> reject(@PathVariable Long orderId) {
        return Result.ok(orderService.reject(orderId));
    }

    @PostMapping("/refund/{orderId}")
    public Result<Orders> refund(@PathVariable Long orderId) {
        return Result.ok(orderService.refund(orderId));
    }
}
