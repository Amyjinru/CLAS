package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review_delete_request")
public class ReviewDeleteRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long replyId;
    private Long merchantId;
    private String requestType;
    private String reporterUserId;
    private String reason;
    private String status;
    private String adminId;
    private String adminRemarks;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
