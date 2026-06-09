package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("deal_redeem_log")
public class DealRedeemLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dealOrderId;
    private Long merchantId;
    private String voucherCode;
    private String operatorId;
    private LocalDateTime redeemedAt;
}
