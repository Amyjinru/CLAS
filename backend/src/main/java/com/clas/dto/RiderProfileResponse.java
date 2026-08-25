package com.clas.dto;

import com.clas.entity.RiderProfile;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Safe rider self-profile view. Ciphertext and emergency-contact data stay server-side. */
public record RiderProfileResponse(
    String userId,
    String realName,
    String idCardMasked,
    String vehicleType,
    String serviceArea,
    Boolean onlineStatus,
    Boolean acceptingOrders,
    Integer maxActiveOrders,
    BigDecimal currentLongitude,
    BigDecimal currentLatitude,
    LocalDateTime locationUpdatedAt,
    Integer withdrawableBalance,
    Integer frozenBalance,
    String status
) {
    public static RiderProfileResponse from(RiderProfile profile) {
        return new RiderProfileResponse(
            profile.getUserId(), profile.getRealName(), profile.getIdCardMasked(), profile.getVehicleType(),
            profile.getServiceArea(), profile.getOnlineStatus(), profile.getAcceptingOrders(), profile.getMaxActiveOrders(),
            profile.getCurrentLongitude(), profile.getCurrentLatitude(), profile.getLocationUpdatedAt(),
            profile.getWithdrawableBalance(), profile.getFrozenBalance(), profile.getStatus()
        );
    }
}
