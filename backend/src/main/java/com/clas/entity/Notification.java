package com.clas.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String title;
    private String content;
    private Boolean readFlag;
    private String type;
    private String targetType;
    private Long targetId;
    private Long reviewId;
    private Long replyId;
    private Long orderId;
    private Long merchantId;
    private String targetPath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
