package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.MerchantRatingResponse;
import com.clas.dto.ReviewDetailResponse;
import com.clas.dto.ReviewReplyCreateRequest;
import com.clas.dto.ReviewReplyResponse;
import com.clas.dto.ReviewRequest;
import com.clas.entity.DeletedReviewBackup;
import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import com.clas.entity.Review;
import com.clas.entity.ReviewDeleteRequest;
import com.clas.entity.ReviewImage;
import com.clas.entity.ReviewReply;
import com.clas.entity.ReviewUserHidden;
import com.clas.entity.ReviewVote;
import com.clas.entity.User;
import com.clas.mapper.DeletedReviewBackupMapper;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ReviewDeleteRequestMapper;
import com.clas.mapper.ReviewImageMapper;
import com.clas.mapper.ReviewMapper;
import com.clas.mapper.ReviewReplyMapper;
import com.clas.mapper.ReviewUserHiddenMapper;
import com.clas.mapper.ReviewVoteMapper;
import com.clas.mapper.UserMapper;
import com.clas.config.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private static final int MAX_IMAGES = 9;
    private static final int BACKUP_LIMIT = 50;

    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final ReviewVoteMapper reviewVoteMapper;
    private final ReviewUserHiddenMapper reviewUserHiddenMapper;
    private final DeletedReviewBackupMapper deletedReviewBackupMapper;
    private final ReviewDeleteRequestMapper reviewDeleteRequestMapper;
    private final OrderService orderService;
    private final OrdersMapper ordersMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantService merchantService;
    private final NotificationService notificationService;
    private final PenaltyService penaltyService;
    private final UserProfileService userProfileService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public ReviewService(
        ReviewMapper reviewMapper,
        ReviewImageMapper reviewImageMapper,
        ReviewReplyMapper reviewReplyMapper,
        ReviewVoteMapper reviewVoteMapper,
        ReviewUserHiddenMapper reviewUserHiddenMapper,
        DeletedReviewBackupMapper deletedReviewBackupMapper,
        ReviewDeleteRequestMapper reviewDeleteRequestMapper,
        OrderService orderService,
        OrdersMapper ordersMapper,
        MerchantMapper merchantMapper,
        MerchantService merchantService,
        NotificationService notificationService,
        PenaltyService penaltyService,
        UserProfileService userProfileService,
        UserMapper userMapper,
        ObjectMapper objectMapper
    ) {
        this.reviewMapper = reviewMapper;
        this.reviewImageMapper = reviewImageMapper;
        this.reviewReplyMapper = reviewReplyMapper;
        this.reviewVoteMapper = reviewVoteMapper;
        this.reviewUserHiddenMapper = reviewUserHiddenMapper;
        this.deletedReviewBackupMapper = deletedReviewBackupMapper;
        this.reviewDeleteRequestMapper = reviewDeleteRequestMapper;
        this.orderService = orderService;
        this.ordersMapper = ordersMapper;
        this.merchantMapper = merchantMapper;
        this.merchantService = merchantService;
        this.notificationService = notificationService;
        this.penaltyService = penaltyService;
        this.userProfileService = userProfileService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Review add(ReviewRequest request) {
        penaltyService.assertCanComment(request.userId());
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
        List<String> images = request.images() == null ? List.of() : request.images();
        if (images.size() > MAX_IMAGES) {
            throw new BusinessException("评价图片最多上传 " + MAX_IMAGES + " 张");
        }
        Review review = new Review();
        review.setOrderId(request.orderId());
        review.setUserId(request.userId());
        review.setScore(request.score());
        review.setContent(request.content());
        review.setReportStatus("NONE");
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
        saveImages(review.getId(), images);
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

    public List<ReviewDetailResponse> listByMerchantId(Long merchantId, String viewerId) {
        List<Long> orderIds = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getMerchantId, merchantId))
            .stream()
            .map(Orders::getId)
            .toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> hiddenIds = hiddenReviewIds(viewerId);
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
            .in(Review::getOrderId, orderIds)
            .orderByDesc(Review::getId));
        return reviews.stream()
            .filter(review -> !hiddenIds.contains(review.getId()))
            .map(review -> toDetail(review, viewerId))
            .toList();
    }

    public List<ReviewDetailResponse> listByUserId(String userId) {
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
            .eq(Review::getUserId, userId)
            .orderByDesc(Review::getId));
        return reviews.stream().map(review -> toDetail(review, userId)).toList();
    }

    public MerchantRatingResponse getMerchantRating(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        List<Review> reviews = listRawByMerchantId(merchantId);
        return new MerchantRatingResponse(merchantId, merchant.getScore(), (long) reviews.size());
    }

    public Review reply(Long reviewId, String reply) {
        penaltyService.assertCanComment(UserContext.getUserId());
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

    public ReviewReply addUserReply(Long reviewId, ReviewReplyCreateRequest request, String userId) {
        penaltyService.assertCanComment(userId);
        Review review = requireReview(reviewId);
        if (request.parentReplyId() != null) {
            ReviewReply parent = reviewReplyMapper.selectById(request.parentReplyId());
            if (parent == null || !parent.getReviewId().equals(reviewId) || Boolean.TRUE.equals(parent.getDeleted())) {
                throw new BusinessException("回复目标不存在");
            }
        }
        ReviewReply reply = new ReviewReply();
        reply.setReviewId(reviewId);
        reply.setParentReplyId(request.parentReplyId());
        reply.setUserId(userId);
        reply.setReplyType("USER");
        reply.setContent(request.content());
        reply.setDeleted(false);
        reply.setCreatedAt(LocalDateTime.now());
        reviewReplyMapper.insert(reply);
        return reply;
    }

    @Transactional
    public ReviewVote vote(Long targetId, String targetType, String voteType, String userId) {
        penaltyService.assertCanComment(userId);
        if (!"LIKE".equals(voteType) && !"DISLIKE".equals(voteType)) {
            throw new BusinessException("投票类型只能是 LIKE 或 DISLIKE");
        }
        validateVoteTarget(targetId, targetType);
        ReviewVote existing = reviewVoteMapper.selectOne(new LambdaQueryWrapper<ReviewVote>()
            .eq(ReviewVote::getTargetType, targetType)
            .eq(ReviewVote::getTargetId, targetId)
            .eq(ReviewVote::getUserId, userId));
        if (existing != null) {
            if (existing.getVoteType().equals(voteType)) {
                reviewVoteMapper.deleteById(existing.getId());
                return null;
            }
            existing.setVoteType(voteType);
            reviewVoteMapper.updateById(existing);
            return existing;
        }
        ReviewVote vote = new ReviewVote();
        vote.setTargetType(targetType);
        vote.setTargetId(targetId);
        vote.setUserId(userId);
        vote.setVoteType(voteType);
        vote.setCreatedAt(LocalDateTime.now());
        reviewVoteMapper.insert(vote);
        return vote;
    }

    @Transactional
    public void deleteOwnReview(Long reviewId, String userId) {
        Review review = requireReview(reviewId);
        if (!review.getUserId().equals(userId)) {
            hideReviewForViewer(reviewId, userId);
            return;
        }
        backupReview(review, userId, "SELF");
        removeReviewData(review);
        Orders order = ordersMapper.selectById(review.getOrderId());
        if (order != null) {
            recalculateMerchantScore(order.getMerchantId());
        }
    }

    @Transactional
    public void adminDeleteReview(Long reviewId) {
        Review review = requireReview(reviewId);
        String adminId = UserContext.getUserId();
        backupReview(review, adminId != null ? adminId : "ADMIN", "ADMIN");
        removeReviewData(review);
        Orders order = ordersMapper.selectById(review.getOrderId());
        if (order != null) {
            recalculateMerchantScore(order.getMerchantId());
        }
    }

    @Transactional
    public void deleteMerchantReply(Long reviewId) {
        Review review = requireReview(reviewId);
        Orders order = orderService.requireOrder(review.getOrderId());
        if (!merchantService.getCurrentMerchantId().equals(order.getMerchantId())) {
            throw new BusinessException("只能删除自己店铺的回复");
        }
        review.setMerchantReply(null);
        reviewVoteMapper.delete(new LambdaQueryWrapper<ReviewVote>()
            .eq(ReviewVote::getTargetType, "MERCHANT_REPLY")
            .eq(ReviewVote::getTargetId, reviewId));
        reviewMapper.updateById(review);
    }

    public ReviewDeleteRequest requestDeleteReview(Long reviewId, String reason) {
        Review review = requireReview(reviewId);
        Orders order = orderService.requireOrder(review.getOrderId());
        Long merchantId = merchantService.getCurrentMerchantId();
        if (!merchantId.equals(order.getMerchantId())) {
            throw new BusinessException("只能为自己店铺的评价发起删除申请");
        }
        ReviewDeleteRequest request = createDeleteRequest(
            reviewId,
            null,
            merchantId,
            reason,
            "MERCHANT",
            null
        );
        notificationService.notifyAdmins("商家申请删除评价", "商家 #" + merchantId + " 申请删除评价 #" + reviewId + "：" + reason);
        return request;
    }

    @Transactional
    public void approveDeleteRequest(Long requestId, String adminId, boolean approve, String remarks) {
        ReviewDeleteRequest request = reviewDeleteRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("删除申请不存在");
        }
        request.setAdminId(adminId);
        request.setAdminRemarks(remarks);
        request.setProcessedAt(LocalDateTime.now());
        request.setStatus(approve ? "APPROVED" : "REJECTED");
        reviewDeleteRequestMapper.updateById(request);
        if (approve) {
            if (request.getReplyId() != null) {
                adminDeleteReply(request.getReplyId());
            } else {
                adminDeleteReview(request.getReviewId());
            }
            Review review = reviewMapper.selectById(request.getReviewId());
            if (review != null && "PENDING".equals(review.getReportStatus())) {
                review.setReportStatus("RESOLVED");
                reviewMapper.updateById(review);
            }
        }
    }

    @Transactional
    public void deleteReply(Long replyId, String userId) {
        ReviewReply reply = reviewReplyMapper.selectById(replyId);
        if (reply == null || Boolean.TRUE.equals(reply.getDeleted())) {
            throw new BusinessException("回复不存在");
        }
        if (!reply.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的回复");
        }
        reply.setDeleted(true);
        reviewReplyMapper.updateById(reply);
    }

    @Transactional
    public void adminDeleteReply(Long replyId) {
        ReviewReply reply = reviewReplyMapper.selectById(replyId);
        if (reply == null || Boolean.TRUE.equals(reply.getDeleted())) {
            throw new BusinessException("回复不存在");
        }
        reply.setDeleted(true);
        reviewReplyMapper.updateById(reply);
    }

    public Review report(Long reviewId, String reason, String userId) {
        Review review = requireReview(reviewId);
        if (review.getUserId().equals(userId)) {
            throw new BusinessException("不能举报自己的评价");
        }
        Orders order = orderService.requireOrder(review.getOrderId());
        createDeleteRequest(reviewId, null, order.getMerchantId(), reason, "USER", userId);
        review.setReportReason(reason);
        review.setReportStatus("PENDING");
        reviewMapper.updateById(review);
        notificationService.notifyAdmins(
            "用户举报评价",
            "用户 " + userId + " 举报评价 #" + reviewId + "：" + reason
        );
        return review;
    }

    public ReviewDeleteRequest reportReply(Long replyId, String reason, String userId) {
        ReviewReply reply = reviewReplyMapper.selectById(replyId);
        if (reply == null || Boolean.TRUE.equals(reply.getDeleted())) {
            throw new BusinessException("回复不存在");
        }
        if (reply.getUserId().equals(userId)) {
            throw new BusinessException("不能举报自己的评论");
        }
        Review review = requireReview(reply.getReviewId());
        Orders order = orderService.requireOrder(review.getOrderId());
        ReviewDeleteRequest request = createDeleteRequest(
            review.getId(),
            replyId,
            order.getMerchantId(),
            reason,
            "USER",
            userId
        );
        notificationService.notifyAdmins(
            "用户举报评论",
            "用户 " + userId + " 举报评价 #" + review.getId() + " 下的回复 #" + replyId + "：" + reason
        );
        return request;
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

    public List<DeletedReviewBackup> listDeletedBackups() {
        return deletedReviewBackupMapper.selectList(new LambdaQueryWrapper<DeletedReviewBackup>()
            .orderByDesc(DeletedReviewBackup::getId)
            .last("LIMIT " + BACKUP_LIMIT));
    }

    public List<ReviewDeleteRequest> listDeleteRequests(String status) {
        LambdaQueryWrapper<ReviewDeleteRequest> wrapper = new LambdaQueryWrapper<ReviewDeleteRequest>()
            .orderByDesc(ReviewDeleteRequest::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ReviewDeleteRequest::getStatus, status);
        }
        return reviewDeleteRequestMapper.selectList(wrapper);
    }

    public void recalculateMerchantScorePublic(Long merchantId) {
        recalculateMerchantScore(merchantId);
    }

    private ReviewDetailResponse toDetail(Review review, String viewerId) {
        User user = userMapper.selectById(review.getUserId());
        VoteSummary reviewVotes = summarizeVotes("REVIEW", review.getId(), viewerId);
        VoteSummary merchantReplyVotes = summarizeVotes("MERCHANT_REPLY", review.getId(), viewerId);
        List<ReviewReply> replies = reviewReplyMapper.selectList(new LambdaQueryWrapper<ReviewReply>()
            .eq(ReviewReply::getReviewId, review.getId())
            .eq(ReviewReply::getDeleted, false)
            .orderByAsc(ReviewReply::getId));
        List<ReviewReplyResponse> replyResponses = replies.stream()
            .map(reply -> toReplyResponse(reply, viewerId))
            .toList();
        return new ReviewDetailResponse(
            review.getId(),
            review.getOrderId(),
            review.getUserId(),
            userProfileService.displayName(user),
            userProfileService.avatarOf(user),
            review.getScore(),
            review.getContent(),
            loadImages(review.getId()),
            review.getMerchantReply(),
            reviewVotes.likes(),
            reviewVotes.dislikes(),
            reviewVotes.myVote(),
            merchantReplyVotes.myVote(),
            merchantReplyVotes.likes(),
            merchantReplyVotes.dislikes(),
            replyResponses,
            review.getCreatedAt() == null ? null : review.getCreatedAt().toString(),
            viewerId != null && viewerId.equals(review.getUserId())
        );
    }

    private ReviewReplyResponse toReplyResponse(ReviewReply reply, String viewerId) {
        User user = userMapper.selectById(reply.getUserId());
        VoteSummary votes = summarizeVotes("REPLY", reply.getId(), viewerId);
        return new ReviewReplyResponse(
            reply.getId(),
            reply.getReviewId(),
            reply.getParentReplyId(),
            reply.getUserId(),
            userProfileService.displayName(user),
            userProfileService.avatarOf(user),
            reply.getReplyType(),
            reply.getContent(),
            votes.likes(),
            votes.dislikes(),
            votes.myVote(),
            reply.getCreatedAt() == null ? null : reply.getCreatedAt().toString(),
            viewerId != null && viewerId.equals(reply.getUserId())
        );
    }

    private VoteSummary summarizeVotes(String targetType, Long targetId, String viewerId) {
        List<ReviewVote> votes = reviewVoteMapper.selectList(new LambdaQueryWrapper<ReviewVote>()
            .eq(ReviewVote::getTargetType, targetType)
            .eq(ReviewVote::getTargetId, targetId));
        long likes = votes.stream().filter(v -> "LIKE".equals(v.getVoteType())).count();
        long dislikes = votes.stream().filter(v -> "DISLIKE".equals(v.getVoteType())).count();
        String myVote = viewerId == null ? null : votes.stream()
            .filter(v -> viewerId.equals(v.getUserId()))
            .map(ReviewVote::getVoteType)
            .findFirst()
            .orElse(null);
        return new VoteSummary(likes, dislikes, myVote);
    }

    private void validateVoteTarget(Long targetId, String targetType) {
        switch (targetType) {
            case "REVIEW" -> requireReview(targetId);
            case "REPLY" -> {
                ReviewReply reply = reviewReplyMapper.selectById(targetId);
                if (reply == null || Boolean.TRUE.equals(reply.getDeleted())) {
                    throw new BusinessException("回复不存在");
                }
            }
            case "MERCHANT_REPLY" -> requireReview(targetId);
            default -> throw new BusinessException("不支持的投票目标");
        }
    }

    private void hideReviewForViewer(Long reviewId, String userId) {
        Long count = reviewUserHiddenMapper.selectCount(new LambdaQueryWrapper<ReviewUserHidden>()
            .eq(ReviewUserHidden::getReviewId, reviewId)
            .eq(ReviewUserHidden::getUserId, userId));
        if (count > 0) {
            return;
        }
        ReviewUserHidden hidden = new ReviewUserHidden();
        hidden.setReviewId(reviewId);
        hidden.setUserId(userId);
        hidden.setCreatedAt(LocalDateTime.now());
        reviewUserHiddenMapper.insert(hidden);
    }

    private Set<Long> hiddenReviewIds(String viewerId) {
        if (viewerId == null) {
            return Set.of();
        }
        return reviewUserHiddenMapper.selectList(new LambdaQueryWrapper<ReviewUserHidden>()
                .eq(ReviewUserHidden::getUserId, viewerId))
            .stream()
            .map(ReviewUserHidden::getReviewId)
            .collect(Collectors.toSet());
    }

    private ReviewDeleteRequest createDeleteRequest(
        Long reviewId,
        Long replyId,
        Long merchantId,
        String reason,
        String requestType,
        String reporterUserId
    ) {
        assertNoPendingRequest(reviewId, replyId);
        ReviewDeleteRequest request = new ReviewDeleteRequest();
        request.setReviewId(reviewId);
        request.setReplyId(replyId);
        request.setMerchantId(merchantId);
        request.setRequestType(requestType);
        request.setReporterUserId(reporterUserId);
        request.setReason(reason);
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());
        reviewDeleteRequestMapper.insert(request);
        return request;
    }

    private void assertNoPendingRequest(Long reviewId, Long replyId) {
        LambdaQueryWrapper<ReviewDeleteRequest> wrapper = new LambdaQueryWrapper<ReviewDeleteRequest>()
            .eq(ReviewDeleteRequest::getReviewId, reviewId)
            .eq(ReviewDeleteRequest::getStatus, "PENDING");
        if (replyId == null) {
            wrapper.isNull(ReviewDeleteRequest::getReplyId);
        } else {
            wrapper.eq(ReviewDeleteRequest::getReplyId, replyId);
        }
        if (reviewDeleteRequestMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该评论已有待审核的删评申请");
        }
    }

    private void backupReview(Review review, String deletedBy, String deleteType) {
        DeletedReviewBackup backup = new DeletedReviewBackup();
        backup.setReviewId(review.getId());
        backup.setUserId(review.getUserId());
        backup.setOrderId(review.getOrderId());
        backup.setScore(review.getScore());
        backup.setContent(review.getContent());
        backup.setImagesJson(toJson(loadImages(review.getId())));
        backup.setDeletedBy(deletedBy);
        backup.setDeleteType(deleteType);
        backup.setDeletedAt(LocalDateTime.now());
        deletedReviewBackupMapper.insert(backup);
        trimBackups();
    }

    private void trimBackups() {
        List<DeletedReviewBackup> backups = deletedReviewBackupMapper.selectList(new LambdaQueryWrapper<DeletedReviewBackup>()
            .orderByDesc(DeletedReviewBackup::getId));
        if (backups.size() <= BACKUP_LIMIT) {
            return;
        }
        backups.subList(BACKUP_LIMIT, backups.size()).forEach(item -> deletedReviewBackupMapper.deleteById(item.getId()));
    }

    private void removeReviewData(Review review) {
        reviewImageMapper.delete(new LambdaQueryWrapper<ReviewImage>().eq(ReviewImage::getReviewId, review.getId()));
        reviewReplyMapper.delete(new LambdaQueryWrapper<ReviewReply>().eq(ReviewReply::getReviewId, review.getId()));
        reviewVoteMapper.delete(new LambdaQueryWrapper<ReviewVote>()
            .and(w -> w.eq(ReviewVote::getTargetType, "REVIEW").eq(ReviewVote::getTargetId, review.getId())
                .or()
                .eq(ReviewVote::getTargetType, "MERCHANT_REPLY").eq(ReviewVote::getTargetId, review.getId())));
        reviewUserHiddenMapper.delete(new LambdaQueryWrapper<ReviewUserHidden>().eq(ReviewUserHidden::getReviewId, review.getId()));
        reviewMapper.deleteById(review.getId());
    }

    private void saveImages(Long reviewId, List<String> images) {
        int order = 0;
        for (String image : images) {
            if (image == null || image.isBlank()) {
                continue;
            }
            ReviewImage entity = new ReviewImage();
            entity.setReviewId(reviewId);
            entity.setImageUrl(image.trim());
            entity.setSortOrder(order++);
            entity.setCreatedAt(LocalDateTime.now());
            reviewImageMapper.insert(entity);
        }
    }

    private List<String> loadImages(Long reviewId) {
        return reviewImageMapper.selectList(new LambdaQueryWrapper<ReviewImage>()
                .eq(ReviewImage::getReviewId, reviewId)
                .orderByAsc(ReviewImage::getSortOrder))
            .stream()
            .map(ReviewImage::getImageUrl)
            .toList();
    }

    private List<Review> listRawByMerchantId(Long merchantId) {
        List<Long> orderIds = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getMerchantId, merchantId))
            .stream()
            .map(Orders::getId)
            .toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
            .in(Review::getOrderId, orderIds));
    }

    private void recalculateMerchantScore(Long merchantId) {
        List<Review> reviews = listRawByMerchantId(merchantId);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            return;
        }
        if (reviews.isEmpty()) {
            merchant.setScore(BigDecimal.ZERO);
            merchantMapper.updateById(merchant);
            return;
        }
        double average = reviews.stream().mapToInt(Review::getScore).average().orElse(0);
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

    private String toJson(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return "[]";
        }
    }

    private record VoteSummary(long likes, long dislikes, String myVote) {
    }
}
