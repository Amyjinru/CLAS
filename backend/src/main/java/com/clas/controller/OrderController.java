package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.CreateOrderRequest;
import com.clas.dto.OrderResponse;
import com.clas.entity.Orders;
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
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public Result<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.create(request));
    }

    @GetMapping("/list/{userId}")
    public Result<List<OrderResponse>> list(@PathVariable Long userId) {
        return Result.ok(orderService.listForUser(userId));
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<List<OrderResponse>> merchantOrders(@PathVariable Long merchantId) {
        return Result.ok(orderService.listForMerchant(merchantId));
    }

    @PostMapping("/pay/{orderId}")
    public Result<Orders> pay(@PathVariable Long orderId) {
        return Result.ok(orderService.pay(orderId));
    }

    @PostMapping("/accept/{orderId}")
    public Result<Orders> accept(@PathVariable Long orderId) {
        return Result.ok(orderService.accept(orderId));
    }

    @PostMapping("/complete/{orderId}")
    public Result<Orders> complete(@PathVariable Long orderId) {
        return Result.ok(orderService.complete(orderId));
    }
}

