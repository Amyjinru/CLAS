package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.GeoUtils;
import com.clas.config.UserContext;
import com.clas.dto.RiderTaskResponse;
import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import com.clas.entity.RiderProfile;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.RiderProfileMapper;
import com.clas.entity.RiderAuditLog;
import com.clas.mapper.RiderAuditLogMapper;
import com.clas.dto.RiderSequenceRequest;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderDispatchService {
    private static final int TASK_RADIUS_METERS = 5000;
    private static final List<String> ACTIVE_STATES = List.of("ASSIGNED_WAITING_MEAL", "DELIVERING");
    private final OrdersMapper orders; private final MerchantMapper merchants; private final RiderProfileMapper profiles; private final RiderLocationService locations; private final RiderAuditLogMapper audits; private final DeliveryTrackingService tracking; private final NotificationService notifications;
    public RiderDispatchService(OrdersMapper orders, MerchantMapper merchants, RiderProfileMapper profiles, RiderLocationService locations, RiderAuditLogMapper audits, DeliveryTrackingService tracking, NotificationService notifications) { this.orders = orders; this.merchants = merchants; this.profiles = profiles; this.locations = locations; this.audits = audits; this.tracking = tracking; this.notifications = notifications; }

    public List<Orders> activeDeliveries() {
        locations.approvedProfile();
        return activeOrders(UserContext.getUserId()).stream()
            .sorted(Comparator.comparing((Orders order) -> "DELIVERING".equals(order.getDeliveryStatus()) ? 0 : 1)
                .thenComparing(Orders::getDeliverySequence, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Orders::getPromiseEndAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    @Transactional
    public List<Orders> reorder(RiderSequenceRequest request) {
        String riderId = UserContext.getUserId();
        locations.approvedProfile();
        List<Orders> active = activeOrders(riderId);
        List<Long> current = active.stream().map(Orders::getId).sorted().toList();
        List<Long> requested = request.orderIds().stream().distinct().sorted().toList();
        if (!current.equals(requested)) throw new BusinessException("排序必须且只能包含本人全部配送中的订单", DomainErrorCode.DELIVERY_FORBIDDEN);
        LocalDateTime eta = LocalDateTime.now();
        for (int i = 0; i < request.orderIds().size(); i++) {
            Long orderId = request.orderIds().get(i);
            Orders order = active.stream().filter(item -> item.getId().equals(orderId)).findFirst().orElseThrow();
            order.setDeliverySequence(i + 1);
            eta = eta.plusMinutes(Math.max(5, order.getEstimatedMinutes() == null ? 15 : order.getEstimatedMinutes()));
            order.setPredictedArrivalAt(eta);
            orders.updateById(order);
            tracking.tracking(order.getId());
        }
        RiderAuditLog audit = new RiderAuditLog();
        audit.setRiderId(riderId); audit.setOperatorId(riderId); audit.setAction("DELIVERY_SEQUENCE_UPDATED");
        audit.setBeforeValue(current.toString()); audit.setAfterValue(request.orderIds().toString()); audit.setCreatedAt(LocalDateTime.now()); audits.insert(audit);
        return activeDeliveries();
    }

    public List<RiderTaskResponse> nearbyTasks() {
        RiderProfile rider = requireOnlineLocated(locations.approvedProfile(), false);
        return orders.selectList(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, "ACCEPTED").eq(Orders::getDeliveryStatus, "AVAILABLE"))
            .stream().map(order -> nearby(order, rider)).filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(RiderTaskResponse::promiseEndAt, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(RiderTaskResponse::merchantDistanceMeters)).toList();
    }

    @Transactional
    public Orders claim(Long orderId) {
        String riderId = UserContext.getUserId();
        RiderProfile rider = requireOnlineLocated(profiles.selectByUserIdForUpdate(riderId), true);
        long active = orders.selectCount(new LambdaQueryWrapper<Orders>().eq(Orders::getRiderId, riderId).in(Orders::getDeliveryStatus, ACTIVE_STATES));
        if (active >= rider.getMaxActiveOrders()) throw new BusinessException("骑手当前配送单已达上限", DomainErrorCode.RIDER_CAPACITY_REACHED);
        Orders order = orders.selectById(orderId);
        if (order == null || !"ACCEPTED".equals(order.getStatus()) || !"AVAILABLE".equals(order.getDeliveryStatus()) || order.getRiderId() != null) throw unavailable();
        Merchant merchant = merchants.selectById(order.getMerchantId());
        if (merchant == null || !GeoUtils.hasCoordinate(merchant.getLongitude(), merchant.getLatitude())) throw unavailable();
        if (GeoUtils.distanceMeters(rider.getCurrentLatitude(), rider.getCurrentLongitude(), merchant.getLatitude(), merchant.getLongitude()) > TASK_RADIUS_METERS) throw unavailable();
        if (orders.claimAvailableTask(orderId, riderId) != 1) throw unavailable();
        Orders claimed = orders.selectById(orderId);
        notifyOrder(claimed, "骑手已接单", "订单已由骑手接单，正在前往商家取餐。");
        return claimed;
    }

    private RiderTaskResponse nearby(Orders order, RiderProfile rider) {
        Merchant merchant = merchants.selectById(order.getMerchantId());
        if (merchant == null || !GeoUtils.hasCoordinate(merchant.getLongitude(), merchant.getLatitude())) return null;
        int distance = GeoUtils.distanceMeters(rider.getCurrentLatitude(), rider.getCurrentLongitude(), merchant.getLatitude(), merchant.getLongitude());
        return distance <= TASK_RADIUS_METERS ? RiderTaskResponse.from(order, merchant, distance, "按承诺送达时间与到店距离排序") : null;
    }
    private List<Orders> activeOrders(String riderId) { return orders.selectList(new LambdaQueryWrapper<Orders>().eq(Orders::getRiderId, riderId).in(Orders::getDeliveryStatus, ACTIVE_STATES)); }
    private RiderProfile requireOnlineLocated(RiderProfile rider, boolean requireAcceptingOrders) {
        if (rider == null || !"APPROVED".equals(rider.getStatus())) throw new BusinessException(403, "骑手身份尚未审核通过或已被停用");
        if (!Boolean.TRUE.equals(rider.getOnlineStatus())) throw new BusinessException("骑手当前未上线");
        if (requireAcceptingOrders && !Boolean.TRUE.equals(rider.getAcceptingOrders())) throw new BusinessException("请先开始接单");
        if (!GeoUtils.hasCoordinate(rider.getCurrentLongitude(), rider.getCurrentLatitude())) throw new BusinessException("请先上报当前位置");
        return rider;
    }
    private BusinessException unavailable() { return new BusinessException("配送任务已被领取或不可用", DomainErrorCode.DELIVERY_TASK_UNAVAILABLE); }
    private void notifyOrder(Orders order, String title, String content) {
        notifications.send(new NotificationService.NotificationTarget(order.getUserId(), title, content, "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/order/" + order.getId()));
        Merchant merchant = merchants.selectById(order.getMerchantId());
        if (merchant != null) notifications.send(new NotificationService.NotificationTarget(merchant.getUserId(), title, "订单 " + order.getId() + "：" + content, "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/merchant-console"));
    }
}
