package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review_reply")
public class ReviewReply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long parentReplyId;
    private String userId;
    private String replyType;
    private String content;
    private Boolean deleted;
    private LocalDateTime createdAt;
}
