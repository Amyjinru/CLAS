package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.config.UserContext;
import com.clas.entity.Orders;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.MerchantMapper;
import com.clas.entity.Merchant;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderDeliveryService {
    private final OrdersMapper orders;
    private final RiderLocationService locations;
    private final MerchantMapper merchants;
    private final NotificationService notifications;
    private final RiderSettlementService settlements;
    public RiderDeliveryService(OrdersMapper orders, RiderLocationService locations, MerchantMapper merchants, NotificationService notifications, RiderSettlementService settlements) { this.orders = orders; this.locations = locations; this.merchants = merchants; this.notifications = notifications; this.settlements = settlements; }

    @Transactional
    public Orders pickup(Long orderId) { return transition(orderId, "ASSIGNED_WAITING_MEAL", "DELIVERING", true); }
    @Transactional
    public Orders deliver(Long orderId) { return transition(orderId, "DELIVERING", "DELIVERED", false); }

    @Transactional
    public Orders abandonBeforePickup(Long orderId, String reason) {
        locations.approvedProfile();
        Orders order = owned(orderId);
        if (!"ASSIGNED_WAITING_MEAL".equals(order.getDeliveryStatus())) throw invalid();
        order.setRiderId(null); order.setRiderAssignedAt(null); order.setDeliveryStatus("AVAILABLE");
        order.setReassignCount((order.getReassignCount() == null ? 0 : order.getReassignCount()) + 1);
        orders.updateById(order);
        notifyOrder(order, "骑手已放弃配送", "骑手暂时无法配送，订单已回到骑手任务池。");
        return order;
    }

    private Orders transition(Long orderId, String expected, String target, boolean pickup) {
        locations.approvedProfile();
        Orders order = owned(orderId);
        if (!expected.equals(order.getDeliveryStatus())) throw invalid();
        LocalDateTime now = LocalDateTime.now();
        order.setDeliveryStatus(target);
        if (pickup) order.setPickedUpAt(now);
        else { order.setDeliveryCompletedAt(now); order.setDeliveredAt(now); }
        orders.updateById(order);
        if (!pickup) settlements.createPendingCommission(order);
        notifyOrder(order, pickup ? "骑手已取餐" : "订单已送达", pickup ? "骑手已取餐，正在配送中。" : "骑手已送达，请确认收货。");
        return order;
    }

    private Orders owned(Long orderId) {
        Orders order = orders.selectById(orderId);
        if (order == null || !UserContext.getUserId().equals(order.getRiderId())) throw new BusinessException("仅已指派骑手可操作配送任务", DomainErrorCode.DELIVERY_FORBIDDEN);
        return order;
    }
    private BusinessException invalid() { return new BusinessException("配送状态不允许此操作", DomainErrorCode.DELIVERY_STATE_INVALID); }
    private void notifyOrder(Orders order, String title, String content) {
        notifications.send(new NotificationService.NotificationTarget(order.getUserId(), title, content, "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/order/" + order.getId()));
        Merchant merchant = merchants.selectById(order.getMerchantId());
        if (merchant != null) notifications.send(new NotificationService.NotificationTarget(merchant.getUserId(), title, "订单 " + order.getId() + "：" + content, "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/merchant-console"));
    }
}
