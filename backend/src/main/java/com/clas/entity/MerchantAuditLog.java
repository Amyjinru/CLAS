package com.clas.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clas.common.MerchantStatusEnum;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("merchant_audit_log")
public class MerchantAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long merchantId;
    private String adminId;
    private MerchantStatusEnum oldStatus;
    private MerchantStatusEnum newStatus;
    private String remarks;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
