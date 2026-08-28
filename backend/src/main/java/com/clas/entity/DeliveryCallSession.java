package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("delivery_call_session")
public class DeliveryCallSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String riderId;
    private String userId;
    private String maskedPhone;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
