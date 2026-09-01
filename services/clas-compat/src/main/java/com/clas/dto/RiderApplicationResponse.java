package com.clas.dto;
import java.time.LocalDateTime;
public record RiderApplicationResponse(Long id, String realName, String idCardMasked, String vehicleType,
    String serviceArea, String status, String rejectReason, LocalDateTime reviewedAt, LocalDateTime createdAt) {}
