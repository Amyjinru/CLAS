package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.OrderResponse;
import com.clas.service.RiderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rider")
@RequireRole("RIDER")
public class RiderController {
    private final RiderService riderService;

    public RiderController(RiderService riderService) {
        this.riderService = riderService;
    }

    @GetMapping("/orders/available")
    public Result<List<OrderResponse>> availableOrders() {
        return Result.ok(riderService.listAvailableOrders());
    }

    @GetMapping("/orders/me")
    public Result<List<OrderResponse>> myOrders() {
        return Result.ok(riderService.listMyOrders(UserContext.getUserId()));
    }

    @PostMapping("/orders/{orderId}/claim")
    public Result<OrderResponse> claim(@PathVariable Long orderId) {
        return Result.ok(riderService.claim(orderId, UserContext.getUserId()));
    }
}
