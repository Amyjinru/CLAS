package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clas.dto.AdminReviewRecord;
import com.clas.dto.InternalPage;
import com.clas.dto.OrderLifecycleEventResponse;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.entity.Review;
import com.clas.mapper.OrderItemMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ReviewMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InternalOrderQueryService {
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;
    private final OrderLifecycleService lifecycleService;

    public InternalOrderQueryService(
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        ReviewMapper reviewMapper,
        OrderLifecycleService lifecycleService
    ) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.reviewMapper = reviewMapper;
        this.lifecycleService = lifecycleService;
    }

    public Orders getOrder(Long orderId) {
        return ordersMapper.selectById(orderId);
    }

    public List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    public List<OrderItem> listItemsByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
    }

    public InternalPage<Orders> listOrders(
        int page,
        int size,
        String status,
        String deliveryStatus,
        LocalDate startDate,
        LocalDate endDate,
        String keyword
    ) {
        Page<Orders> result = ordersMapper.selectPage(new Page<>(page, size), orderQuery(status, deliveryStatus, startDate, endDate, keyword));
        return new InternalPage<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<Orders> exportOrders(
        String status,
        String deliveryStatus,
        LocalDate startDate,
        LocalDate endDate,
        String keyword
    ) {
        return ordersMapper.selectList(orderQuery(status, deliveryStatus, startDate, endDate, keyword));
    }

    public List<Orders> listAvailablePreparing() {
        return ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getStatus, "ACCEPTED")
            .eq(Orders::getDeliveryStatus, "PREPARING")
            .isNull(Orders::getRiderId)
            .orderByAsc(Orders::getAcceptedAt));
    }

    public List<Orders> listAvailableDispatch() {
        return ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getStatus, "ACCEPTED")
            .eq(Orders::getDeliveryStatus, "AVAILABLE"));
    }

    public List<Orders> listByRider(String riderId) {
        return ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getRiderId, riderId)
            .orderByDesc(Orders::getRiderAcceptedAt));
    }

    public List<Orders> listByRiderAndDeliveryStatuses(String riderId, Collection<String> deliveryStatuses) {
        if (riderId == null || riderId.isBlank() || deliveryStatuses == null || deliveryStatuses.isEmpty()) {
            return List.of();
        }
        return ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getRiderId, riderId)
            .in(Orders::getDeliveryStatus, deliveryStatuses));
    }

    public long countByRiderAndDeliveryStatuses(String riderId, Collection<String> deliveryStatuses) {
        if (riderId == null || riderId.isBlank() || deliveryStatuses == null || deliveryStatuses.isEmpty()) {
            return 0L;
        }
        return ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getRiderId, riderId)
            .in(Orders::getDeliveryStatus, deliveryStatuses));
    }

    public List<Orders> listMaturedDelivered(LocalDateTime cutoff) {
        return ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getDeliveryStatus, "DELIVERED")
            .le(Orders::getDeliveryCompletedAt, cutoff)
            .in(Orders::getStatus, "ACCEPTED", "COMPLETED")
            .and(wrapper -> wrapper.isNull(Orders::getRefundStatus)
                .or().in(Orders::getRefundStatus, "NONE", "REJECTED")));
    }

    public List<OrderLifecycleEventResponse> listLifecycle(Long orderId) {
        return lifecycleService.list(orderId);
    }

    public InternalPage<AdminReviewRecord> listReviews(int page, int size, String reportStatus, String keyword) {
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, size), reviewQuery(reportStatus, keyword));
        return new InternalPage<>(toAdminReviews(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<AdminReviewRecord> exportReviews(String reportStatus, String keyword) {
        return toAdminReviews(reviewMapper.selectList(reviewQuery(reportStatus, keyword)));
    }

    private List<AdminReviewRecord> toAdminReviews(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = reviews.stream().map(Review::getOrderId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Long> merchantByOrder = orderIds.isEmpty()
            ? Map.of()
            : ordersMapper.selectList(new LambdaQueryWrapper<Orders>().in(Orders::getId, orderIds))
                .stream()
                .collect(Collectors.toMap(Orders::getId, Orders::getMerchantId, (a, b) -> a));
        return reviews.stream()
            .map(review -> new AdminReviewRecord(
                review.getId(),
                review.getOrderId(),
                review.getUserId(),
                review.getScore(),
                review.getContent(),
                review.getMerchantReply(),
                review.getReportReason(),
                review.getReportStatus(),
                merchantByOrder.get(review.getOrderId()),
                review.getCreatedAt()
            ))
            .toList();
    }

    private LambdaQueryWrapper<Orders> orderQuery(
        String status,
        String deliveryStatus,
        LocalDate startDate,
        LocalDate endDate,
        String keyword
    ) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Orders::getStatus, status);
        }
        if (deliveryStatus != null && !deliveryStatus.isBlank()) {
            wrapper.eq(Orders::getDeliveryStatus, deliveryStatus);
        }
        if (startDate != null) {
            wrapper.ge(Orders::getCreateTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(Orders::getCreateTime, endDate.atTime(LocalTime.MAX));
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(w -> {
                w.like(Orders::getUserId, normalizedKeyword)
                    .or()
                    .like(Orders::getDeliveryAddress, normalizedKeyword);
                try {
                    Long numeric = Long.valueOf(normalizedKeyword);
                    w.or().eq(Orders::getId, numeric).or().eq(Orders::getMerchantId, numeric);
                } catch (NumberFormatException ignored) {
                    // Non-numeric keywords only search text fields.
                }
            });
        }
        wrapper.orderByDesc(Orders::getCreateTime);
        return wrapper;
    }

    private LambdaQueryWrapper<Review> reviewQuery(String reportStatus, String keyword) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (reportStatus != null && !reportStatus.isBlank()) {
            wrapper.eq(Review::getReportStatus, reportStatus);
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(w -> w.like(Review::getUserId, normalizedKeyword)
                .or()
                .like(Review::getContent, normalizedKeyword)
                .or()
                .like(Review::getReportReason, normalizedKeyword));
        }
        wrapper.orderByDesc(Review::getId);
        return wrapper;
    }
}
