package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("deal_order")
public class DealOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dealId;
    private String userId;
    private Long merchantId;
    private String voucherCode;
    private String status;
    private Integer payAmount;
    private LocalDateTime paidTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime usedTime;

    @TableField(exist = false)
    private String dealTitle;

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private String merchantLogo;
}
