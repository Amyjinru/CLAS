package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** Immutable audit trail for every meaningful order and delivery transition. */
@Data
@TableName("order_lifecycle_event")
public class OrderLifecycleEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private String fromDeliveryStatus;
    private String toDeliveryStatus;
    private String actorRole;
    private String actorId;
    private String remark;
    private LocalDateTime createdAt;
}
