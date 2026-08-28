package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.config.UserContext;
import com.clas.entity.DealOrder;
import com.clas.entity.Merchant;
import com.clas.entity.Notification;
import com.clas.entity.Orders;
import com.clas.entity.Review;
import com.clas.entity.ServiceBooking;
import com.clas.mapper.DealOrderMapper;
import com.clas.entity.User;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.NotificationMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ReviewMapper;
import com.clas.mapper.ServiceBookingMapper;
import com.clas.mapper.UserMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final OrdersMapper ordersMapper;
    private final ReviewMapper reviewMapper;
    private final DealOrderMapper dealOrderMapper;
    private final ServiceBookingMapper bookingMapper;
    private final MerchantMapper merchantMapper;

    public NotificationService(
        NotificationMapper notificationMapper,
        UserMapper userMapper,
        OrdersMapper ordersMapper,
        ReviewMapper reviewMapper,
        DealOrderMapper dealOrderMapper,
        ServiceBookingMapper bookingMapper,
        MerchantMapper merchantMapper
    ) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.ordersMapper = ordersMapper;
        this.reviewMapper = reviewMapper;
        this.dealOrderMapper = dealOrderMapper;
        this.bookingMapper = bookingMapper;
        this.merchantMapper = merchantMapper;
    }

    public void send(String userId, String title, String content) {
        send(new NotificationTarget(
            userId,
            title,
            content,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    public void send(NotificationTarget target) {
        Notification notification = new Notification();
        notification.setUserId(target.userId());
        notification.setTitle(target.title());
        notification.setContent(target.content());
        notification.setReadFlag(false);
        notification.setType(target.type());
        notification.setTargetType(target.targetType());
        notification.setTargetId(target.targetId());
        notification.setReviewId(target.reviewId());
        notification.setReplyId(target.replyId());
        notification.setOrderId(target.orderId());
        notification.setMerchantId(target.merchantId());
        notification.setTargetPath(target.targetPath());
        notificationMapper.insert(notification);
    }

    public void notifyAdmins(String title, String content) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
            .eq(User::getRole, "ADMIN"));
        for (User admin : admins) {
            send(admin.getPhone(), title, content);
        }
    }

    public List<Notification> mine() {
        List<Notification> notifications = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
            .eq(Notification::getUserId, UserContext.getUserId())
            .orderByAsc(Notification::getReadFlag)
            .orderByDesc(Notification::getId));
        notifications.forEach(this::backfillLegacyTarget);
        return notifications;
    }

    public void markRead(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification != null && UserContext.getUserId().equals(notification.getUserId())) {
            notification.setReadFlag(true);
            notificationMapper.updateById(notification);
        }
    }

    @Transactional
    public void markAllRead() {
        for (Notification notification : mine()) {
            if (!Boolean.TRUE.equals(notification.getReadFlag())) {
                notification.setReadFlag(true);
                notificationMapper.updateById(notification);
            }
        }
    }

    public void deleteOne(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !UserContext.getUserId().equals(notification.getUserId())) {
            throw new BusinessException("通知不存在或无权删除");
        }
        notificationMapper.deleteById(id);
    }

    public void deleteAllMine() {
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
            .eq(Notification::getUserId, UserContext.getUserId()));
    }

    private void backfillLegacyTarget(Notification notification) {
        if (hasTarget(notification)) {
            return;
        }
        boolean updated = backfillParsedTarget(notification)
            || backfillBookingTarget(notification);
        if (updated) {
            notificationMapper.updateById(notification);
        }
    }

    private boolean hasTarget(Notification notification) {
        return notification.getTargetType() != null && notification.getTargetPath() != null;
    }

    private boolean backfillParsedTarget(Notification notification) {
        Optional<LegacyNotificationTargetResolver.LegacyTarget> resolved =
            LegacyNotificationTargetResolver.resolve(notification);
        if (resolved.isEmpty()) {
            return false;
        }

        LegacyNotificationTargetResolver.LegacyTarget target = resolved.get();
        return switch (target.targetType()) {
            case "ORDER" -> backfillOrderTarget(notification, target.primaryId());
            case "REVIEW" -> backfillReviewTarget(notification, target.primaryId());
            case "DEAL_ORDER" -> backfillDealOrderTarget(notification, target.reference());
            default -> false;
        };
    }

    private boolean backfillOrderTarget(Notification notification, Long orderId) {
        if (orderId == null) {
            return false;
        }
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !notification.getUserId().equals(order.getUserId())) {
            return false;
        }
        notification.setType("ORDER_STATUS");
        notification.setTargetType("ORDER");
        notification.setTargetId(order.getId());
        notification.setOrderId(order.getId());
        notification.setMerchantId(order.getMerchantId());
        notification.setTargetPath("/order/" + order.getId());
        return true;
    }

    private boolean backfillReviewTarget(Notification notification, Long orderId) {
        if (orderId == null) {
            return false;
        }
        Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
            .eq(Review::getOrderId, orderId)
            .eq(Review::getUserId, notification.getUserId())
            .orderByDesc(Review::getId)
            .last("LIMIT 1"));
        if (review == null) {
            return false;
        }
        Orders order = ordersMapper.selectById(orderId);
        notification.setType("MERCHANT_REVIEW_REPLY");
        notification.setTargetType("REVIEW");
        notification.setTargetId(review.getId());
        notification.setReviewId(review.getId());
        notification.setOrderId(orderId);
        notification.setMerchantId(order == null ? null : order.getMerchantId());
        notification.setTargetPath("/review/" + orderId + "?reviewId=" + review.getId());
        return true;
    }

    private boolean backfillDealOrderTarget(Notification notification, String voucherCode) {
        LambdaQueryWrapper<DealOrder> query = new LambdaQueryWrapper<DealOrder>()
            .eq(DealOrder::getUserId, notification.getUserId());
        if (voucherCode != null && !voucherCode.isBlank()) {
            query.eq(DealOrder::getVoucherCode, voucherCode);
        } else if (notification.getCreatedAt() != null) {
            query.le(DealOrder::getCreateTime, notification.getCreatedAt().plusSeconds(2));
        }
        DealOrder order = dealOrderMapper.selectOne(query
            .orderByDesc(DealOrder::getId)
            .last("LIMIT 1"));
        if (order == null) {
            return false;
        }
        notification.setType("DEAL_ORDER_STATUS");
        notification.setTargetType("DEAL_ORDER");
        notification.setTargetId(order.getId());
        notification.setOrderId(order.getId());
        notification.setMerchantId(order.getMerchantId());
        notification.setTargetPath("/deal-order/" + order.getId());
        return true;
    }

    private boolean backfillBookingTarget(Notification notification) {
        String title = notification.getTitle();
        if (!List.of("预约已提交", "新的预约申请", "预约已取消", "预约状态更新").contains(title)) {
            return false;
        }
        List<Long> merchantIds = merchantIdsForUser(notification.getUserId());
        LambdaQueryWrapper<ServiceBooking> query = new LambdaQueryWrapper<ServiceBooking>();
        if (merchantIds.isEmpty()) {
            query.eq(ServiceBooking::getUserId, notification.getUserId());
        } else {
            query.and(wrapper -> wrapper
                .eq(ServiceBooking::getUserId, notification.getUserId())
                .or()
                .in(ServiceBooking::getMerchantId, merchantIds));
        }
        List<ServiceBooking> bookings = bookingMapper.selectList(query.orderByDesc(ServiceBooking::getId));
        ServiceBooking booking = bookings.stream()
            .filter(candidate -> notificationContentStartsWith(notification, candidate.getServiceName()))
            .findFirst()
            .orElse(null);
        if (booking == null) {
            return false;
        }
        notification.setType("BOOKING_STATUS");
        notification.setTargetType("BOOKING");
        notification.setTargetId(booking.getId());
        notification.setMerchantId(booking.getMerchantId());
        notification.setTargetPath(booking.getUserId().equals(notification.getUserId())
            ? "/bookings?bookingId=" + booking.getId()
            : "/merchant/bookings?bookingId=" + booking.getId());
        return true;
    }

    private List<Long> merchantIdsForUser(String userId) {
        return merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId))
            .stream()
            .map(Merchant::getId)
            .toList();
    }

    private boolean notificationContentStartsWith(Notification notification, String prefix) {
        return prefix != null
            && notification.getContent() != null
            && notification.getContent().startsWith(prefix);
    }

    public record NotificationTarget(
        String userId,
        String title,
        String content,
        String type,
        String targetType,
        Long targetId,
        Long reviewId,
        Long replyId,
        Long orderId,
        Long merchantId,
        String targetPath
    ) {
    }
}
