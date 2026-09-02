package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.dto.InternalDeliveryCommands.AbandonRequest;
import com.clas.dto.InternalDeliveryCommands.ClaimRequest;
import com.clas.dto.InternalDeliveryCommands.LifecycleEventRequest;
import com.clas.dto.InternalDeliveryCommands.PredictedArrivalRequest;
import com.clas.dto.InternalDeliveryCommands.SequenceItem;
import com.clas.dto.InternalDeliveryCommands.SequenceRequest;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.mapper.OrderItemMapper;
import com.clas.mapper.OrdersMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalOrderDeliveryService {
    private static final String MODE_AVAILABLE = "AVAILABLE";
    private static final String MODE_PREPARING = "PREPARING";

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLifecycleService lifecycleService;

    public InternalOrderDeliveryService(
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        OrderLifecycleService lifecycleService
    ) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.lifecycleService = lifecycleService;
    }

    public Orders getOrder(Long orderId) {
        return requireOrder(orderId);
    }

    public List<OrderItem> listItems(Long orderId) {
        requireOrder(orderId);
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    @Transactional
    public Orders claim(Long orderId, ClaimRequest request) {
        String riderId = requireRiderId(request == null ? null : request.riderId());
        String mode = request.mode() == null || request.mode().isBlank()
            ? MODE_AVAILABLE
            : request.mode().trim().toUpperCase();
        return switch (mode) {
            case MODE_PREPARING -> claimPreparing(orderId, riderId);
            case MODE_AVAILABLE -> claimAvailable(orderId, riderId);
            default -> throw new BusinessException("不支持的领取模式: " + mode);
        };
    }

    @Transactional
    public Orders pickup(Long orderId, String riderId) {
        return transition(orderId, requireRiderId(riderId), "ASSIGNED_WAITING_MEAL", "DELIVERING", true);
    }

    @Transactional
    public Orders complete(Long orderId, String riderId) {
        return transition(orderId, requireRiderId(riderId), "DELIVERING", "DELIVERED", false);
    }

    @Transactional
    public Orders abandon(Long orderId, AbandonRequest request) {
        String riderId = requireRiderId(request == null ? null : request.riderId());
        Orders order = requireOwned(orderId, riderId);
        if (!"ASSIGNED_WAITING_MEAL".equals(order.getDeliveryStatus())) {
            throw invalidState();
        }
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        order.setRiderId(null);
        order.setRiderAssignedAt(null);
        order.setDeliveryStatus("AVAILABLE");
        order.setReassignCount((order.getReassignCount() == null ? 0 : order.getReassignCount()) + 1);
        ordersMapper.updateById(order);
        ordersMapper.clearRiderAssignment(order.getId());
        order = requireOrder(orderId);
        lifecycleService.record(
            order,
            "RIDER_ABANDONED",
            fromStatus,
            fromDelivery,
            "RIDER",
            riderId,
            request == null || request.reason() == null || request.reason().isBlank() ? "骑手放弃配送" : request.reason()
        );
        return order;
    }

    @Transactional
    public Orders updatePredictedArrival(Long orderId, PredictedArrivalRequest request) {
        Orders order = requireOrder(orderId);
        if (request == null || request.predictedArrivalAt() == null) {
            throw new BusinessException("预计到达时间不能为空");
        }
        order.setPredictedArrivalAt(request.predictedArrivalAt());
        ordersMapper.updateById(order);
        return order;
    }

    @Transactional
    public List<Orders> updateDeliverySequence(String riderId, SequenceRequest request) {
        String owner = requireRiderId(riderId);
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BusinessException("配送排序不能为空");
        }
        for (SequenceItem item : request.items()) {
            if (item == null || item.orderId() == null) {
                throw new BusinessException("排序项缺少订单号");
            }
            Orders order = requireOwned(item.orderId(), owner);
            if (item.sequence() != null) {
                order.setDeliverySequence(item.sequence());
            }
            if (item.predictedArrivalAt() != null) {
                order.setPredictedArrivalAt(item.predictedArrivalAt());
            }
            ordersMapper.updateById(order);
        }
        return request.items().stream().map(item -> requireOrder(item.orderId())).toList();
    }

    @Transactional
    public void recordLifecycle(LifecycleEventRequest request) {
        if (request == null || request.orderId() == null || request.eventType() == null || request.eventType().isBlank()) {
            throw new BusinessException("生命周期事件不完整");
        }
        Orders order = requireOrder(request.orderId());
        lifecycleService.record(
            order,
            request.eventType(),
            request.fromStatus() == null ? order.getStatus() : request.fromStatus(),
            request.fromDeliveryStatus() == null ? order.getDeliveryStatus() : request.fromDeliveryStatus(),
            request.actorRole() == null ? "SYSTEM" : request.actorRole(),
            request.actorId(),
            request.remark()
        );
    }

    private Orders claimAvailable(Long orderId, String riderId) {
        Orders before = requireOrder(orderId);
        if (ordersMapper.claimAvailableTask(orderId, riderId) != 1) {
            throw new BusinessException("配送任务已被领取或不可用", DomainErrorCode.DELIVERY_TASK_UNAVAILABLE);
        }
        Orders claimed = requireOrder(orderId);
        lifecycleService.record(claimed, "RIDER_CLAIMED", before.getStatus(), before.getDeliveryStatus(), "RIDER", riderId, "骑手领取配送任务");
        return claimed;
    }

    private Orders claimPreparing(Long orderId, String riderId) {
        int updated = ordersMapper.claimForRider(orderId, riderId);
        if (updated == 0) {
            if (ordersMapper.selectById(orderId) == null) {
                throw new BusinessException("订单不存在", DomainErrorCode.RESOURCE_NOT_FOUND);
            }
            throw new BusinessException("订单已被其他骑手接走或状态已变化");
        }
        return requireOrder(orderId);
    }

    private Orders transition(Long orderId, String riderId, String expected, String target, boolean pickup) {
        Orders order = requireOwned(orderId, riderId);
        if (!expected.equals(order.getDeliveryStatus())) {
            throw invalidState();
        }
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        LocalDateTime now = LocalDateTime.now();
        order.setDeliveryStatus(target);
        if (pickup) {
            order.setPickedUpAt(now);
        } else {
            order.setDeliveryCompletedAt(now);
            order.setDeliveredAt(now);
        }
        ordersMapper.updateById(order);
        lifecycleService.record(
            order,
            pickup ? "RIDER_PICKED_UP" : "RIDER_DELIVERED",
            fromStatus,
            fromDelivery,
            "RIDER",
            riderId,
            pickup ? "骑手确认取餐" : "骑手确认送达"
        );
        return order;
    }

    private Orders requireOwned(Long orderId, String riderId) {
        Orders order = requireOrder(orderId);
        if (!riderId.equals(order.getRiderId())) {
            throw new BusinessException("仅已指派骑手可操作配送任务", DomainErrorCode.DELIVERY_FORBIDDEN);
        }
        return order;
    }

    private Orders requireOrder(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在", DomainErrorCode.RESOURCE_NOT_FOUND);
        }
        return order;
    }

    private String requireRiderId(String riderId) {
        if (riderId == null || riderId.isBlank()) {
            throw new BusinessException("骑手身份不能为空");
        }
        return riderId;
    }

    private BusinessException invalidState() {
        return new BusinessException("配送状态不允许此操作", DomainErrorCode.DELIVERY_STATE_INVALID);
    }
}
