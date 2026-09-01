package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rider_application")
public class RiderApplication {
    @TableId(type = IdType.AUTO) private Long id;
    private String userId; private String realName; private String idCardCiphertext; private String idCardMasked;
    private String vehicleType; private String serviceArea; private String emergencyContactName; private String emergencyContactPhone;
    private String credentialUrls; private String status; private String rejectReason; private String reviewerId;
    private LocalDateTime reviewedAt; private LocalDateTime createdAt;
}
