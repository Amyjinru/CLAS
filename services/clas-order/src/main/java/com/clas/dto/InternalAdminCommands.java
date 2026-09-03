package com.clas.dto;

public final class InternalAdminCommands {
    private InternalAdminCommands() {
    }

    public record ReviewReportStatusRequest(String status) {
    }

    public record ProcessDeleteRequest(boolean approve, String remarks, String adminId) {
    }

    public record RefundDisputeAuditCommand(boolean approved, String reason, String adminId) {
    }
}
