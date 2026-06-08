package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.MerchantRatingResponse;
import com.clas.dto.ReviewReplyRequest;
import com.clas.dto.ReviewReportRequest;
import com.clas.dto.ReviewRequest;
import com.clas.entity.Review;
import com.clas.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/add")
    @RequireRole("USER")
    public Result<Review> add(@Valid @RequestBody ReviewRequest request) {
        return Result.ok(reviewService.add(new ReviewRequest(
            request.orderId(),
            UserContext.getUserId(),
            request.score(),
            request.content()
        )));
    }

    @GetMapping("/order/{orderId}")
    @RequireRole("USER")
    public Result<Review> getByOrder(@PathVariable Long orderId) {
        return Result.ok(reviewService.getByOrderId(orderId, UserContext.getUserId()));
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<List<Review>> listByMerchant(@PathVariable Long merchantId) {
        return Result.ok(reviewService.listByMerchantId(merchantId));
    }

    @GetMapping("/rating/{merchantId}")
    public Result<MerchantRatingResponse> merchantRating(@PathVariable Long merchantId) {
        return Result.ok(reviewService.getMerchantRating(merchantId));
    }

    @PostMapping("/{reviewId}/reply")
    @RequireRole("MERCHANT")
    public Result<Review> reply(@PathVariable Long reviewId, @Valid @RequestBody ReviewReplyRequest request) {
        return Result.ok(reviewService.reply(reviewId, request.reply()));
    }

    @PostMapping("/{reviewId}/report")
    @RequireRole("USER")
    public Result<Review> report(@PathVariable Long reviewId, @Valid @RequestBody ReviewReportRequest request) {
        return Result.ok(reviewService.report(reviewId, request.reason(), UserContext.getUserId()));
    }
}
