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
    private String status;
    private String deliveryAddress;
    private BigDecimal deliveryLongitude;
    private BigDecimal deliveryLatitude;
    private Integer distanceMeters;
    private Integer routeDistanceMeters;
    private String deliveryStatus;
    private Integer estimatedMinutes;
    private String refundReason;
    private String refundStatus;
    private LocalDateTime createTime;
}
