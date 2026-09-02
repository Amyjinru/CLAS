package com.clas.dto;

public record AppealProcessRequest(
    String status,
    String adminReply,
    String adminId
) {
}
