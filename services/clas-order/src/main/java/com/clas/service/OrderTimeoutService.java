package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.entity.Orders;
import com.clas.mapper.OrdersMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderTimeoutService {
    private final OrdersMapper ordersMapper;
    private final CouponService couponService;
    private final NotificationBridge notificationBridge;
    private final boolean enabled;
    private final int pendingPaymentTimeoutMinutes;

    public OrderTimeoutService(
        OrdersMapper ordersMapper,
        CouponService couponService,
        NotificationBridge notificationBridge,
        @Value("${app.order-timeout.enabled:true}") boolean enabled,
        @Value("${app.order-timeout.pending-payment-minutes:30}") int pendingPaymentTimeoutMinutes
    ) {
        this.ordersMapper = ordersMapper;
        this.couponService = couponService;
        this.notificationBridge = notificationBridge;
        this.enabled = enabled;
        this.pendingPaymentTimeoutMinutes = pendingPaymentTimeoutMinutes;
    }

    @Scheduled(fixedDelayString = "${app.order-timeout.scan-delay-ms:60000}")
    public void expirePendingPaymentOrders() {
        if (!enabled) {
            return;
        }
        expirePendingPaymentOrders(LocalDateTime.now());
    }

    @Transactional
    public int expirePendingPaymentOrders(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(pendingPaymentTimeoutMinutes);
        List<Orders> expiredOrders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getStatus, OrderService.STATUS_PENDING_PAYMENT)
            .le(Orders::getCreateTime, cutoff)
            .orderByAsc(Orders::getCreateTime)
            .last("LIMIT 100"));

        int expired = 0;
        for (Orders order : expiredOrders) {
            int updated = ordersMapper.updateStatusIfCurrent(
                order.getId(),
                OrderService.STATUS_PENDING_PAYMENT,
                OrderService.STATUS_CANCELED
            );
            if (updated == 0) {
                continue;
            }
            order.setStatus(OrderService.STATUS_CANCELED);
            order.setCanceledAt(now);
            ordersMapper.updateById(order);
            couponService.releaseForOrder(order.getUserCouponId());
            notifyUser(order);
            expired++;
        }
        return expired;
    }

    private void notifyUser(Orders order) {
        notificationBridge.send(new NotificationBridge.NotificationTarget(
            order.getUserId(),
            "订单已超时取消",
            "订单 " + order.getId() + " 因超过 " + pendingPaymentTimeoutMinutes + " 分钟未支付，已自动取消。",
            "ORDER_STATUS",
            "ORDER",
            order.getId(),
            null,
            null,
            order.getId(),
            order.getMerchantId(),
            "/orders?orderId=" + order.getId()
        ));
    }
}
