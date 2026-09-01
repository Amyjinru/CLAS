package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String userId;
    private Integer amount;
    private String payMethod;
    private String status;
    private LocalDateTime createTime;
    private String idempotencyKey;
}
