package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.MerchantRatingResponse;
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
    public Result<Review> add(@Valid @RequestBody ReviewRequest request) {
        return Result.ok(reviewService.add(request));
    }

    @GetMapping("/order/{orderId}")
    public Result<Review> getByOrder(@PathVariable Long orderId) {
        return Result.ok(reviewService.getByOrderId(orderId));
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<List<Review>> listByMerchant(@PathVariable Long merchantId) {
        return Result.ok(reviewService.listByMerchantId(merchantId));
    }

    @GetMapping("/rating/{merchantId}")
    public Result<MerchantRatingResponse> merchantRating(@PathVariable Long merchantId) {
        return Result.ok(reviewService.getMerchantRating(merchantId));
    }
}
