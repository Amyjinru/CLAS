package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.ReviewRequest;
import com.clas.entity.Orders;
import com.clas.entity.Review;
import com.clas.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {
    private final ReviewMapper reviewMapper;
    private final OrderService orderService;

    public ReviewService(ReviewMapper reviewMapper, OrderService orderService) {
        this.reviewMapper = reviewMapper;
        this.orderService = orderService;
    }

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
        reviewMapper.insert(review);
        return review;
    }
}

