package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rider_settlement")
public class RiderSettlement {
    @TableId(type = IdType.AUTO) private Long id;
    private String riderId; private Long orderId; private String sourceType; private String sourceId;
    private String settlementType; private Integer amount; private String balanceType; private LocalDateTime createdAt;
}
