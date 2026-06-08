package com.clas.controller;

import com.clas.common.BusinessException;
import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.MerchantRatingResponse;
import com.clas.dto.ReviewDeleteRequestDto;
import com.clas.dto.ReviewDetailResponse;
import com.clas.dto.ReviewReplyCreateRequest;
import com.clas.dto.ReviewReplyRequest;
import com.clas.dto.ReviewReportRequest;
import com.clas.dto.ReviewRequest;
import com.clas.dto.ReviewVoteRequest;
import com.clas.entity.Review;
import com.clas.entity.ReviewDeleteRequest;
import com.clas.entity.ReviewReply;
import com.clas.entity.ReviewVote;
import com.clas.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
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
            request.content(),
            request.images()
        )));
    }

    @GetMapping("/order/{orderId}")
    @RequireRole("USER")
    public Result<Review> getByOrder(@PathVariable Long orderId) {
        return Result.ok(reviewService.getByOrderId(orderId, UserContext.getUserId()));
    }

    @GetMapping("/mine")
    @RequireRole("USER")
    public Result<List<ReviewDetailResponse>> listMine() {
        return Result.ok(reviewService.listByUserId(UserContext.getUserId()));
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<List<ReviewDetailResponse>> listByMerchant(@PathVariable Long merchantId) {
        return Result.ok(reviewService.listByMerchantId(merchantId, UserContext.getUserId()));
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

    @PostMapping("/{reviewId}/comments")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<ReviewReply> addComment(@PathVariable Long reviewId, @Valid @RequestBody ReviewReplyCreateRequest request) {
        return Result.ok(reviewService.addUserReply(reviewId, request, UserContext.getUserId()));
    }

    @PostMapping("/{targetType}/{targetId}/vote")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<ReviewVote> vote(
        @PathVariable String targetType,
        @PathVariable Long targetId,
        @Valid @RequestBody ReviewVoteRequest request
    ) {
        return Result.ok(reviewService.vote(targetId, targetType.toUpperCase(), request.voteType(), UserContext.getUserId()));
    }

    @DeleteMapping("/{reviewId}")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<Void> deleteReview(@PathVariable Long reviewId) {
        String role = UserContext.getRole();
        if ("ADMIN".equals(role)) {
            reviewService.adminDeleteReview(reviewId);
        } else if ("MERCHANT".equals(role)) {
            throw new BusinessException("商家请通过删评申请流程删除评价");
        } else {
            reviewService.deleteOwnReview(reviewId, UserContext.getUserId());
        }
        return Result.ok();
    }

    @DeleteMapping("/{reviewId}/merchant-reply")
    @RequireRole("MERCHANT")
    public Result<Void> deleteMerchantReply(@PathVariable Long reviewId) {
        reviewService.deleteMerchantReply(reviewId);
        return Result.ok();
    }

    @DeleteMapping("/reply/{replyId}")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<Void> deleteReply(@PathVariable Long replyId) {
        reviewService.deleteReply(replyId, UserContext.getUserId());
        return Result.ok();
    }

    @PostMapping("/{reviewId}/delete-request")
    @RequireRole("MERCHANT")
    public Result<ReviewDeleteRequest> requestDelete(@PathVariable Long reviewId, @Valid @RequestBody ReviewDeleteRequestDto request) {
        return Result.ok(reviewService.requestDeleteReview(reviewId, request.reason()));
    }

    @PostMapping("/{reviewId}/report")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<Review> report(@PathVariable Long reviewId, @Valid @RequestBody ReviewReportRequest request) {
        return Result.ok(reviewService.report(reviewId, request.reason(), UserContext.getUserId()));
    }

    @PostMapping("/reply/{replyId}/report")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<ReviewDeleteRequest> reportReply(@PathVariable Long replyId, @Valid @RequestBody ReviewReportRequest request) {
        return Result.ok(reviewService.reportReply(replyId, request.reason(), UserContext.getUserId()));
    }
}
