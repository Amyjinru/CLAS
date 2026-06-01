package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.OrderResponse;
import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import com.clas.service.MerchantService;
import com.clas.service.OrderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public Result<List<Merchant>> list() {
        return Result.ok(merchantService.list());
    }

    @GetMapping("/{id}")
    public Result<Merchant> detail(@PathVariable Long id) {
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
}

