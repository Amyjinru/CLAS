package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.ReviewRequest;
import com.clas.entity.Review;
import com.clas.service.ReviewService;
import jakarta.validation.Valid;
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
}

