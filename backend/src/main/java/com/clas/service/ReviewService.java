package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.MerchantRatingResponse;
import com.clas.dto.ReviewRequest;
import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import com.clas.entity.Review;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ReviewMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewMapper reviewMapper;
    private final OrderService orderService;
    private final OrdersMapper ordersMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantService merchantService;
    private final NotificationService notificationService;

    public ReviewService(
        ReviewMapper reviewMapper,
        OrderService orderService,
        OrdersMapper ordersMapper,
        MerchantMapper merchantMapper,
        MerchantService merchantService,
        NotificationService notificationService
    ) {
        this.reviewMapper = reviewMapper;
        this.orderService = orderService;
        this.ordersMapper = ordersMapper;
        this.merchantMapper = merchantMapper;
        this.merchantService = merchantService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Review add(ReviewRequest request) {
        Orders order = orderService.requireOrder(request.orderId());
        if (!request.userId().equals(order.getUserId())) {
            throw new BusinessException("只能评价自己的订单");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("订单完成后才能评价");
        }
        Long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
            .eq(Review::getOrderId, request.orderId()));
        if (count > 0) {
            throw new BusinessException("该订单已评价");
        }
        Review review = new Review();
        review.setOrderId(request.orderId());
        review.setUserId(request.userId());
        review.setScore(request.score());
        review.setContent(request.content());
        review.setReportStatus("NONE");
        reviewMapper.insert(review);
        recalculateMerchantScore(order.getMerchantId());
        return review;
    }

    public Review getByOrderId(Long orderId) {
        return reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
            .eq(Review::getOrderId, orderId));
    }

    public Review getByOrderId(Long orderId, String userId) {
        orderService.requireUserOrder(orderId, userId);
        return getByOrderId(orderId);
    }

    public List<Review> listByMerchantId(Long merchantId) {
        List<Long> orderIds = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getMerchantId, merchantId))
            .stream()
            .map(Orders::getId)
            .toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
            .in(Review::getOrderId, orderIds)
            .orderByDesc(Review::getId));
    }

    public MerchantRatingResponse getMerchantRating(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        List<Review> reviews = listByMerchantId(merchantId);
        return new MerchantRatingResponse(
            merchantId,
            merchant.getScore(),
            (long) reviews.size()
        );
    }

    public Review reply(Long reviewId, String reply) {
        Review review = requireReview(reviewId);
        Orders order = orderService.requireOrder(review.getOrderId());
        if (!merchantService.getCurrentMerchantId().equals(order.getMerchantId())) {
            throw new BusinessException("只能回复自己店铺的评价");
        }
        review.setMerchantReply(reply);
        reviewMapper.updateById(review);
        notificationService.send(review.getUserId(), "商家回复了评价", "您的订单 " + review.getOrderId() + " 评价收到商家回复。");
        return review;
    }

    public Review report(Long reviewId, String reason, String userId) {
        Review review = requireReview(reviewId);
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("只能举报自己的评价记录");
        }
        review.setReportReason(reason);
        review.setReportStatus("PENDING");
        reviewMapper.updateById(review);
        return review;
    }

    public Review resolveReport(Long reviewId, String status) {
        Review review = requireReview(reviewId);
        String nextStatus = status == null || status.isBlank() ? "RESOLVED" : status;
        if (!"RESOLVED".equals(nextStatus) && !"REJECTED".equals(nextStatus) && !"PENDING".equals(nextStatus)) {
            throw new BusinessException("举报状态只能是 PENDING、RESOLVED 或 REJECTED");
        }
        review.setReportStatus(nextStatus);
        reviewMapper.updateById(review);
        return review;
    }

    /**
     * 公开的重算商家评分方法 — 供 AdminController 删除评价后调用
     */
    public void recalculateMerchantScorePublic(Long merchantId) {
        recalculateMerchantScore(merchantId);
    }

    private void recalculateMerchantScore(Long merchantId) {
        List<Review> reviews = listByMerchantId(merchantId);
        if (reviews.isEmpty()) {
            return;
        }
        double average = reviews.stream()
            .mapToInt(Review::getScore)
            .average()
            .orElse(0);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            return;
        }
        merchant.setScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        merchantMapper.updateById(merchant);
    }

    private Review requireReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        return review;
    }
}
