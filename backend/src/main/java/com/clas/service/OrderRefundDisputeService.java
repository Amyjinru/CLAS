package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.entity.Merchant;
import com.clas.entity.OrderRefundDispute;
import com.clas.entity.Orders;
import com.clas.mapper.MerchantMapper;
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
    private final MerchantMapper merchants;
    private final OrderService orderService;
    private final OrderLifecycleService lifecycle;
    private final RiderSettlementService settlements;
    private final NotificationService notifications;

    public OrderRefundDisputeService(OrderRefundDisputeMapper disputes, OrdersMapper orders, MerchantMapper merchants,
                                     OrderService orderService, OrderLifecycleService lifecycle,
                                     RiderSettlementService settlements, NotificationService notifications) {
        this.disputes = disputes;
        this.orders = orders;
        this.merchants = merchants;
        this.orderService = orderService;
        this.lifecycle = lifecycle;
        this.settlements = settlements;
        this.notifications = notifications;
    }

    @Transactional
    public OrderRefundDispute submit(Long orderId, String userId, String reason) {
        Orders order = orderService.requireUserOrder(orderId, userId);
        if (!"REJECTED".equals(order.getRefundStatus())) {
            throw new BusinessException("请先等待商家处理退款申请；仅商家拒绝后的订单可提交争议");
        }
        return createPendingDispute(order, reason, "USER", userId, "用户提交退款争议：");
    }

    /** Creates the platform case immediately when a merchant rejects a refund. */
    @Transactional
    public OrderRefundDispute autoSubmitAfterMerchantReject(Orders order, String merchantActorId) {
        if (!"REJECTED".equals(order.getRefundStatus())) {
            throw new BusinessException("订单当前不处于商家拒绝退款状态");
        }
        String userReason = order.getRefundReason();
        if (userReason == null || userReason.isBlank()) userReason = "用户未补充退款原因";
        return createPendingDispute(order, userReason, "MERCHANT", merchantActorId, "商家拒绝退款后自动转入平台争议：");
    }

    private OrderRefundDispute createPendingDispute(
        Orders order, String userReason, String actorRole, String actorId, String lifecycleRemarkPrefix
    ) {
        Long orderId = order.getId();
        if (disputes.selectCount(new LambdaQueryWrapper<OrderRefundDispute>()
            .eq(OrderRefundDispute::getOrderId, orderId).eq(OrderRefundDispute::getStatus, "PENDING")) > 0) {
            throw new BusinessException("该订单已有待审核的退款争议");
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
        notifications.notifyAdmins("待处理订单退款争议", "订单 " + orderId + " 的退款争议等待审核。");
        notifyUser(order, "退款申请已进入平台审核", "订单 " + orderId + " 的退款争议已转交管理员审核。");
        notifyMerchant(order, "退款争议已进入平台审核", "订单 " + orderId + " 的退款争议已转交管理员审核。");
        notifyRider(order, "订单退款争议处理中", "订单 " + orderId + " 的配送佣金暂不进入可提现余额。");
        return dispute;
    }

    public List<OrderRefundDispute> list(String status) {
        LambdaQueryWrapper<OrderRefundDispute> query = new LambdaQueryWrapper<OrderRefundDispute>()
            .orderByAsc(OrderRefundDispute::getStatus)
            .orderByDesc(OrderRefundDispute::getCreatedAt);
        if (status != null && !status.isBlank()) query.eq(OrderRefundDispute::getStatus, status);
        return disputes.selectList(query);
    }

    @Transactional
    public OrderRefundDispute audit(Long disputeId, boolean approved, String reason, String adminId) {
        OrderRefundDispute dispute = disputes.selectById(disputeId);
        if (dispute == null) throw new BusinessException("退款争议不存在");
        if (!"PENDING".equals(dispute.getStatus())) throw new BusinessException("该退款争议已处理");
        Orders order = orders.selectById(dispute.getOrderId());
        if (order == null || !"DISPUTE_PENDING".equals(order.getRefundStatus())) {
            throw new BusinessException("订单当前不处于待裁定退款争议状态");
        }

        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        if (approved) {
            orderService.approveRefundByAdmin(order, adminId, reason.trim());
            dispute.setStatus("APPROVED");
            notifyMerchant(order, "退款争议已裁定", "订单 " + order.getId() + " 的退款争议已通过，平台将按退款处理。");
        } else {
            order.setStatus(dispute.getOriginalOrderStatus());
            order.setDeliveryStatus(dispute.getOriginalDeliveryStatus());
            order.setRefundStatus("REJECTED");
            order.setRefundResolvedAt(LocalDateTime.now());
            order.setRefundRejectReason("管理员裁定：" + reason.trim());
            orders.updateById(order);
            lifecycle.record(order, "REFUND_DISPUTE_REJECTED", fromStatus, fromDelivery, "ADMIN", adminId, "管理员驳回退款争议：" + reason.trim());
            settlements.releaseCommissionIfEligible(order);
            notifyUser(order, "退款争议未通过", "订单 " + order.getId() + " 的退款争议未通过：" + reason.trim());
            notifyMerchant(order, "退款争议已裁定", "订单 " + order.getId() + " 的退款争议未通过。");
        }
        dispute.setAdminReason(reason.trim());
        dispute.setReviewerId(adminId);
        dispute.setReviewedAt(LocalDateTime.now());
        disputes.updateById(dispute);
        return dispute;
    }

    private void notifyUser(Orders order, String title, String content) {
        notifications.send(new NotificationService.NotificationTarget(order.getUserId(), title, content, "ORDER_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/order/" + order.getId()));
    }

    private void notifyMerchant(Orders order, String title, String content) {
        Merchant merchant = merchants.selectById(order.getMerchantId());
        if (merchant != null) notifications.send(new NotificationService.NotificationTarget(merchant.getUserId(), title, content, "ORDER_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/merchant-console"));
    }

    private void notifyRider(Orders order, String title, String content) {
        if (order.getRiderId() != null) notifications.send(new NotificationService.NotificationTarget(order.getRiderId(), title, content, "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/rider/profile"));
    }
}
