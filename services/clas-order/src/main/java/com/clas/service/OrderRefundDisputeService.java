package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.CatalogClient;
import com.clas.client.MerchantClient;
import com.clas.client.CompatClient;
import com.clas.common.BusinessException;
import com.clas.entity.Merchant;
import com.clas.entity.OrderRefundDispute;
import com.clas.entity.Orders;
import com.clas.mapper.OrderRefundDisputeMapper;
import com.clas.mapper.OrdersMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderRefundDisputeService {
    private final OrderRefundDisputeMapper disputes;
    private final OrdersMapper orders;
    private final CatalogClient catalogClient;
    private final MerchantClient merchantClient;
    private final OrderService orderService;
    private final OrderLifecycleService lifecycle;
    private final CompatClient compatClient;
    private final NotificationBridge notifications;

    public OrderRefundDisputeService(
        OrderRefundDisputeMapper disputes,
        OrdersMapper orders,
        CatalogClient catalogClient,
        MerchantClient merchantClient,
        OrderService orderService,
        OrderLifecycleService lifecycle,
        CompatClient compatClient,
        NotificationBridge notifications
    ) {
        this.disputes = disputes;
        this.orders = orders;
        this.catalogClient = catalogClient;
        this.merchantClient = merchantClient;
        this.orderService = orderService;
        this.lifecycle = lifecycle;
        this.compatClient = compatClient;
        this.notifications = notifications;
    }

    @Transactional
    public OrderRefundDispute submit(Long orderId, String userId, String reason) {
        Orders order = orderService.requireUserOrder(orderId, userId);
        if (!"REJECTED".equals(order.getRefundStatus())) {
            throw new BusinessException("???????????????????????????");
        }
        return createPendingDispute(order, reason, "USER", userId, "?????????");
    }

    @Transactional
    public OrderRefundDispute autoSubmitAfterMerchantReject(Orders order, String merchantActorId) {
        if (!"REJECTED".equals(order.getRefundStatus())) {
            throw new BusinessException("???????????????");
        }
        String userReason = order.getRefundReason();
        if (userReason == null || userReason.isBlank()) {
            userReason = "?????????";
        }
        return createPendingDispute(order, userReason, "MERCHANT", merchantActorId, "????????????????");
    }

    private OrderRefundDispute createPendingDispute(
        Orders order, String userReason, String actorRole, String actorId, String lifecycleRemarkPrefix
    ) {
        Long orderId = order.getId();
        if (disputes.selectCount(new LambdaQueryWrapper<OrderRefundDispute>()
            .eq(OrderRefundDispute::getOrderId, orderId).eq(OrderRefundDispute::getStatus, "PENDING")) > 0) {
            throw new BusinessException("?????????????");
        }
        OrderRefundDispute dispute = new OrderRefundDispute();
        dispute.setOrderId(orderId);
        dispute.setUserId(order.getUserId());
        dispute.setMerchantId(order.getMerchantId());
        dispute.setRiderId(order.getRiderId());
        dispute.setStatus("PENDING");
        dispute.setUserReason(userReason.trim());
        dispute.setMerchantRejectReason(order.getRefundRejectReason());
        dispute.setOriginalOrderStatus(order.getStatus());
        dispute.setOriginalDeliveryStatus(order.getDeliveryStatus());
        dispute.setCreatedAt(LocalDateTime.now());
        disputes.insert(dispute);

        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        order.setStatus(OrderService.STATUS_REFUND_PENDING);
        order.setRefundStatus("DISPUTE_PENDING");
        order.setRefundResolvedAt(null);
        orders.updateById(order);
        String eventType = "MERCHANT".equals(actorRole) ? "REFUND_DISPUTE_AUTO_SUBMITTED" : "REFUND_DISPUTE_SUBMITTED";
        lifecycle.record(order, eventType, fromStatus, fromDelivery, actorRole, actorId, lifecycleRemarkPrefix + dispute.getUserReason());
        notifications.notifyAdmins("?????????", "?? " + orderId + " ??????????");
        notifyUser(order, "???????????", "?? " + orderId + " ??????????????");
        notifyMerchant(order, "???????????", "?? " + orderId + " ??????????????");
        notifyRider(order, "?????????", "?? " + orderId + " ???????????????");
        return dispute;
    }

    public List<OrderRefundDispute> list(String status) {
        LambdaQueryWrapper<OrderRefundDispute> query = new LambdaQueryWrapper<OrderRefundDispute>()
            .orderByAsc(OrderRefundDispute::getStatus)
            .orderByDesc(OrderRefundDispute::getCreatedAt);
        if (status != null && !status.isBlank()) {
            query.eq(OrderRefundDispute::getStatus, status);
        }
        return disputes.selectList(query);
    }

    @Transactional
    public OrderRefundDispute audit(Long disputeId, boolean approved, String reason, String adminId) {
        OrderRefundDispute dispute = disputes.selectById(disputeId);
        if (dispute == null) {
            throw new BusinessException("???????");
        }
        if (!"PENDING".equals(dispute.getStatus())) {
            throw new BusinessException("????????");
        }
        Orders order = orders.selectById(dispute.getOrderId());
        if (order == null || !"DISPUTE_PENDING".equals(order.getRefundStatus())) {
            throw new BusinessException("????????????????");
        }

        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        if (approved) {
            orderService.approveRefundByAdmin(order, adminId, reason.trim());
            dispute.setStatus("APPROVED");
            notifyMerchant(order, "???????", "?? " + order.getId() + " ??????????????????");
        } else {
            order.setStatus(dispute.getOriginalOrderStatus());
            order.setDeliveryStatus(dispute.getOriginalDeliveryStatus());
            order.setRefundStatus("REJECTED");
            order.setRefundResolvedAt(LocalDateTime.now());
            order.setRefundRejectReason("??????" + reason.trim());
            orders.updateById(order);
            lifecycle.record(order, "REFUND_DISPUTE_REJECTED", fromStatus, fromDelivery, "ADMIN", adminId, "??????????" + reason.trim());
            compatClient.releaseCommissionIfEligible(order);
            notifyUser(order, "???????", "?? " + order.getId() + " ?????????" + reason.trim());
            notifyMerchant(order, "???????", "?? " + order.getId() + " ?????????");
        }
        dispute.setAdminReason(reason.trim());
        dispute.setReviewerId(adminId);
        dispute.setReviewedAt(LocalDateTime.now());
        disputes.updateById(dispute);
        return dispute;
    }

    private void notifyUser(Orders order, String title, String content) {
        notifications.send(new NotificationBridge.NotificationTarget(
            order.getUserId(), title, content, "ORDER_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/order/" + order.getId()));
    }

    private void notifyMerchant(Orders order, String title, String content) {
        Merchant merchant = merchantClient.getMerchant(order.getMerchantId());
        if (merchant != null) {
            notifications.send(new NotificationBridge.NotificationTarget(
                merchant.getUserId(), title, content, "ORDER_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/merchant-console"));
        }
    }

    private void notifyRider(Orders order, String title, String content) {
        if (order.getRiderId() != null) {
            notifications.send(new NotificationBridge.NotificationTarget(
                order.getRiderId(), title, content, "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/rider/profile"));
        }
    }
}
