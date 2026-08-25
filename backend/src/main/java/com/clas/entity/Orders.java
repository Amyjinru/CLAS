package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private Long merchantId;
    private Integer totalPrice;
    private Integer subtotal;
    private Integer deliveryFee;
    private Integer couponDiscount;
    private Long userCouponId;
    private String status;
    private String deliveryAddress;
    private BigDecimal deliveryLongitude;
    private BigDecimal deliveryLatitude;
    private Integer distanceMeters;
    private Integer routeDistanceMeters;
    private String deliveryStatus;
    private String riderId;
    private LocalDateTime riderAcceptedAt;
    private Integer estimatedMinutes;
    private String refundReason;
    private String refundStatus;
    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundResolvedAt;
    private String remark;
    private String rejectReason;
    private String refundRejectReason;
    private LocalDateTime createTime;
    private LocalDateTime paidAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime rejectedAt;
}
