package com.clas.dto;

import java.util.List;

public record ReviewDetailResponse(
    Long id,
    Long orderId,
    String userId,
    String displayName,
    String avatar,
    Integer score,
    String content,
    List<String> images,
    String merchantReply,
    Long likeCount,
    Long dislikeCount,
    String myVote,
    String merchantReplyVote,
    Long merchantReplyLikeCount,
    Long merchantReplyDislikeCount,
    List<ReviewReplyResponse> replies,
    String createdAt,
    Boolean mine
) {
}
