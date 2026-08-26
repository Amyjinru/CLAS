package com.clas.dto;

public record RiderInfoResponse(
    String userId,
    String displayName,
    String avatar,
    String realName,
    String idCardMasked,
    String vehicleType,
    String serviceArea,
    String emergencyContactName,
    String emergencyContactPhone,
    String servicePhone,
    String status,
    Boolean onlineStatus,
    Boolean acceptingOrders,
    Integer maxActiveOrders,
    Double averageRating,
    Long ratingCount,
    RiderPhoneChangeResponse latestPhoneChange
) {
}
