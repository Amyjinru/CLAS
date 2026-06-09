package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String couponType;
    private Integer discountAmount;
    private Integer discountPercent;
    private Integer minOrderAmount;
    private Long merchantId;
    private Integer totalLimit;
    private Integer claimedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String status;
    private LocalDateTime createdAt;
}
