package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.entity.Orders;
import com.clas.entity.RiderReview;
import com.clas.mapper.RiderReviewMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderReviewService {
    private final RiderReviewMapper reviews;
    private final NotificationBridge notifications;
    private final OrderLifecycleService lifecycleService;

    public RiderReviewService(RiderReviewMapper reviews, NotificationBridge notifications, OrderLifecycleService lifecycleService) {
        this.reviews = reviews;
        this.notifications = notifications;
        this.lifecycleService = lifecycleService;
    }

    @Transactional
    public RiderReview create(Orders order, String userId, Integer score, String tags, String content) {
        if (!"COMPLETED".equals(order.getStatus()) || order.getRiderId() == null) throw new BusinessException("确认收货后才可评价骑手");
        if (reviews.selectOne(new LambdaQueryWrapper<RiderReview>().eq(RiderReview::getOrderId, order.getId())) != null) throw new BusinessException("该订单已评价骑手");
        RiderReview review = new RiderReview();
        review.setOrderId(order.getId()); review.setUserId(userId); review.setRiderId(order.getRiderId());
        review.setScore(score); review.setTags(tags); review.setContent(content); review.setCreatedAt(LocalDateTime.now());
        reviews.insert(review);
        lifecycleService.record(order, "RIDER_REVIEWED", order.getStatus(), order.getDeliveryStatus(), "USER", userId, "用户完成骑手评价");
        notifications.send(new NotificationBridge.NotificationTarget(order.getRiderId(), "收到骑手评价", "订单 " + order.getId() + " 收到新的服务评价。", "RIDER_REVIEW", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/rider-workbench"));
        return review;
    }

    public List<RiderReview> mine(String riderId) {
        return reviews.selectList(new LambdaQueryWrapper<RiderReview>().eq(RiderReview::getRiderId, riderId).orderByDesc(RiderReview::getCreatedAt));
    }

    public RiderReview getForOrder(Orders order) {
        return reviews.selectOne(new LambdaQueryWrapper<RiderReview>().eq(RiderReview::getOrderId, order.getId()));
    }
}
