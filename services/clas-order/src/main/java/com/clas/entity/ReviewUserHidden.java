package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review_user_hidden")
public class ReviewUserHidden {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private String userId;
    private LocalDateTime createdAt;
}
