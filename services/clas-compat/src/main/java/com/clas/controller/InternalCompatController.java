package com.clas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.OrderClient;
import com.clas.common.Result;
import com.clas.dto.RoleApplicationRecordResponse;
import com.clas.entity.Orders;
import com.clas.entity.RiderApplication;
import com.clas.mapper.RiderApplicationMapper;
import com.clas.service.RiderApplicationService;
import com.clas.service.RiderSettlementService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/compat/v1")
public class InternalCompatController {
    private final RiderApplicationMapper riderApplicationMapper;
    private final RiderApplicationService riderApplicationService;
    private final RiderSettlementService riderSettlementService;
    private final OrderClient orderClient;

    public InternalCompatController(
        RiderApplicationMapper riderApplicationMapper,
        RiderApplicationService riderApplicationService,
        RiderSettlementService riderSettlementService,
        OrderClient orderClient
    ) {
        this.riderApplicationMapper = riderApplicationMapper;
        this.riderApplicationService = riderApplicationService;
        this.riderSettlementService = riderSettlementService;
        this.orderClient = orderClient;
    }

    @GetMapping("/users/{userId}/rider-pending")
    public Result<Boolean> riderPending(@PathVariable String userId) {
        boolean pending = riderApplicationMapper.exists(new LambdaQueryWrapper<RiderApplication>()
            .eq(RiderApplication::getUserId, userId)
            .eq(RiderApplication::getStatus, "PENDING"));
        return Result.ok(pending);
    }

    @GetMapping("/users/{userId}/rider-application-records")
    public Result<List<RoleApplicationRecordResponse>> riderApplicationRecords(@PathVariable String userId) {
        return Result.ok(riderApplicationService.listApplicationRecords(userId));
    }

    @PostMapping("/orders/{orderId}/commission/withdrawable")
    public Result<Void> makeCommissionWithdrawable(@PathVariable Long orderId) {
        riderSettlementService.makeCommissionWithdrawable(requireOrder(orderId));
        return Result.ok();
    }

    @PostMapping("/orders/{orderId}/commission/reverse")
    public Result<Void> reverseCommissionForRefund(@PathVariable Long orderId) {
        riderSettlementService.reverseCommissionForRefund(requireOrder(orderId));
        return Result.ok();
    }

    @PostMapping("/orders/{orderId}/commission/release")
    public Result<Void> releaseCommissionIfEligible(@PathVariable Long orderId) {
        riderSettlementService.releaseCommissionIfEligible(requireOrder(orderId));
        return Result.ok();
    }

    private Orders requireOrder(Long orderId) {
        Orders order = orderClient.getOrder(orderId);
        if (order == null) {
            throw new com.clas.common.BusinessException("订单不存在");
        }
        return order;
    }
}
