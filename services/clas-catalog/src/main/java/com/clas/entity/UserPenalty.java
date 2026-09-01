package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_penalty")
public class UserPenalty {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String penaltyType;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String adminId;
    private Boolean active;
    private LocalDateTime createdAt;
}
