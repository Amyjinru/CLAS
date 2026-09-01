package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.config.UserContext;
import com.clas.dto.RiderOrderDetailResponse;
import com.clas.entity.Merchant;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.entity.Product;
import com.clas.client.CatalogClient;
import com.clas.client.MerchantClient;
import com.clas.mapper.OrderItemMapper;
import com.clas.mapper.OrdersMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderDeliveryService {
    private final OrdersMapper orders;
    private final RiderLocationService locations;
    private final MerchantClient merchantClient;
    private final NotificationBridge notifications;
    private final RiderSettlementService settlements;
    private final OrderLifecycleService lifecycleService;
    private final OrderItemMapper orderItems;
    private final CatalogClient catalogClient;
    public RiderDeliveryService(OrdersMapper orders, RiderLocationService locations, MerchantClient merchantClient, NotificationBridge notifications, RiderSettlementService settlements, OrderLifecycleService lifecycleService, OrderItemMapper orderItems, CatalogClient catalogClient) { this.orders = orders; this.locations = locations; this.merchantClient = merchantClient; this.notifications = notifications; this.settlements = settlements; this.lifecycleService = lifecycleService; this.orderItems = orderItems; this.catalogClient = catalogClient; }

    @Transactional
    public Orders pickup(Long orderId) { return transition(orderId, "ASSIGNED_WAITING_MEAL", "DELIVERING", true); }
    @Transactional
    public Orders deliver(Long orderId) { return transition(orderId, "DELIVERING", "DELIVERED", false); }

    @Transactional
    public Orders abandonBeforePickup(Long orderId, String reason) {
        locations.approvedProfile();
        Orders order = owned(orderId);
        if (!"ASSIGNED_WAITING_MEAL".equals(order.getDeliveryStatus())) throw invalid();
        String fromStatus = order.getStatus(); String fromDelivery = order.getDeliveryStatus();
        order.setRiderId(null); order.setRiderAssignedAt(null); order.setDeliveryStatus("AVAILABLE");
        order.setReassignCount((order.getReassignCount() == null ? 0 : order.getReassignCount()) + 1);
        orders.updateById(order);
        orders.clearRiderAssignment(order.getId());
        lifecycleService.record(order, "RIDER_ABANDONED", fromStatus, fromDelivery, "RIDER", UserContext.getUserId(), reason);
        notifyOrder(order, "骑手已放弃配送", "骑手暂时无法配送，订单已回到骑手任务池。");
        return order;
    }

    private Orders transition(Long orderId, String expected, String target, boolean pickup) {
        locations.approvedProfile();
        Orders order = owned(orderId);
        if (!expected.equals(order.getDeliveryStatus())) throw invalid();
        String fromStatus = order.getStatus(); String fromDelivery = order.getDeliveryStatus();
        LocalDateTime now = LocalDateTime.now();
        order.setDeliveryStatus(target);
        if (pickup) order.setPickedUpAt(now);
        else { order.setDeliveryCompletedAt(now); order.setDeliveredAt(now); }
        orders.updateById(order);
        lifecycleService.record(order, pickup ? "RIDER_PICKED_UP" : "RIDER_DELIVERED", fromStatus, fromDelivery, "RIDER", UserContext.getUserId(), pickup ? "骑手确认取餐" : "骑手确认送达");
        if (!pickup) settlements.createPendingCommission(order);
        notifyOrder(order, pickup ? "骑手已取餐" : "订单已送达", pickup ? "骑手已取餐，正在配送中。" : "骑手已送达，请确认收货。");
        return order;
    }

    private Orders owned(Long orderId) {
        Orders order = orders.selectById(orderId);
        if (order == null || !UserContext.getUserId().equals(order.getRiderId())) throw new BusinessException("仅已指派骑手可操作配送任务", DomainErrorCode.DELIVERY_FORBIDDEN);
        return order;
    }

    /** 骑手查看「订单详情」：订单 + 商家 + 带商品名的餐品明细。 */
    public RiderOrderDetailResponse detail(Long orderId) {
        Orders order = owned(orderId);
        Merchant merchant = merchantClient.getMerchant(order.getMerchantId());
        List<OrderItem> items = orderItems.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        List<Long> productIds = items.stream().map(OrderItem::getProductId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameByProductId = catalogClient.getProducts(productIds).values().stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        List<RiderOrderDetailResponse.Item> detailItems = items.stream()
            .map(item -> new RiderOrderDetailResponse.Item(
                item.getProductId(),
                nameByProductId.getOrDefault(item.getProductId(), "商品#" + item.getProductId()),
                item.getQuantity(),
                item.getPrice()))
            .toList();
        return new RiderOrderDetailResponse(order, merchant, detailItems);
    }
    private BusinessException invalid() { return new BusinessException("配送状态不允许此操作", DomainErrorCode.DELIVERY_STATE_INVALID); }
    private void notifyOrder(Orders order, String title, String content) {
        notifications.send(new NotificationBridge.NotificationTarget(order.getUserId(), title, content, "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/order/" + order.getId()));
        Merchant merchant = merchantClient.getMerchant(order.getMerchantId());
        if (merchant != null) notifications.send(new NotificationBridge.NotificationTarget(merchant.getUserId(), title, "订单 " + order.getId() + "：" + content, "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/merchant-console"));
    }
}
