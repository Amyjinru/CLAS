package com.clas.service;

import com.clas.entity.Orders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps merchant refund handling and the automatic platform-dispute creation atomic. */
@Service
public class RefundResolutionService {
    private final OrderService orderService;
    private final OrderRefundDisputeService disputeService;

    public RefundResolutionService(OrderService orderService, OrderRefundDisputeService disputeService) {
        this.orderService = orderService;
        this.disputeService = disputeService;
    }

    @Transactional
    public Orders resolveByMerchant(Long orderId, Long merchantId, boolean approved, String rejectReason) {
        Orders order = orderService.resolveRefund(orderId, merchantId, approved, rejectReason);
        if (!approved) disputeService.autoSubmitAfterMerchantReject(order, String.valueOf(merchantId));
        return order;
    }
}
