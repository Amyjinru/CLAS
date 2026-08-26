package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rider_profile_change_request")
public class RiderProfileChangeRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String riderId;
    private String currentPhone;
    private String requestedPhone;
    private String status;
    private String reviewReason;
    private String reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
