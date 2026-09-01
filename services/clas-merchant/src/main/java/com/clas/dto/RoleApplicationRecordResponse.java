package com.clas.dto;

import java.time.LocalDateTime;

/** A unified view of a rider or merchant identity application for the applicant. */
public record RoleApplicationRecordResponse(
    String id,
    String targetRole,
    String status,
    String reason,
    String adminRemarks,
    String operatorId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
