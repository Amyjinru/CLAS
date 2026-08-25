package com.clas.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rider_profile")
public class RiderProfile {
    @TableId private String userId;
    private String realName; private String idCardCiphertext; private String idCardMasked; private String vehicleType;
    private String serviceArea; private String emergencyContactName; private String emergencyContactPhone;
    private Boolean onlineStatus; private Boolean acceptingOrders; private Integer maxActiveOrders; private BigDecimal currentLongitude; private BigDecimal currentLatitude;
    private LocalDateTime locationUpdatedAt; private Integer withdrawableBalance; private Integer frozenBalance; private String status;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
