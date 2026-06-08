package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("appeal")
public class Appeal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private Long penaltyId;
    private String content;
    private String status;
    private String adminReply;
    private String adminId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
