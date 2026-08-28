package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rider_location_history")
public class RiderLocationHistory {
    @TableId(type = IdType.AUTO) private Long id;
    private String riderId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer accuracyMeters;
    private LocalDateTime reportedAt;
}
