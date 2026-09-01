package com.clas.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clas.common.MerchantStatusEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String userId;
    private String merchantName;
    private String logo;
    private String phone;
    private String category;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer deliveryRadiusM;
    private String businessHours;
    private Integer deliveryFee;
    private Integer minOrderPrice;
    private Integer averagePrice;
    private BigDecimal score;
    private MerchantStatusEnum status;
    private Boolean manualClosed;
    private String bankAccount;
    private String adminRemarks;
    private Integer settlementCycle;
    private Integer defaultPrepareMinutes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
