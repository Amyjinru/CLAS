package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("deleted_review_backup")
public class DeletedReviewBackup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private String userId;
    private Long orderId;
    private Integer score;
    private String content;
    private String imagesJson;
    private String deletedBy;
    private String deleteType;
    private LocalDateTime deletedAt;
}
