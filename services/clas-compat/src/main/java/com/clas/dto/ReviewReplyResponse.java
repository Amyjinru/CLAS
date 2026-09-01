package com.clas.dto;

public record ReviewReplyResponse(
    Long id,
    Long reviewId,
    Long parentReplyId,
    String userId,
    String displayName,
    String avatar,
    String replyType,
    String content,
    Long likeCount,
    Long dislikeCount,
    String myVote,
    String createdAt,
    Boolean mine
) {
}
