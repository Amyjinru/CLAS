package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** Administrative record created only after a merchant has rejected a refund request. */
@Data
@TableName("order_refund_dispute")
public class OrderRefundDispute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String userId;
    private Long merchantId;
    private String riderId;
    private String status;
    private String userReason;
    private String merchantRejectReason;
    private String originalOrderStatus;
    private String originalDeliveryStatus;
    private String adminReason;
    private String reviewerId;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
