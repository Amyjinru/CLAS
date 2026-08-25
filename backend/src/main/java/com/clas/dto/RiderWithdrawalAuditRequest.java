package com.clas.dto;
import jakarta.validation.constraints.NotBlank;
public record RiderWithdrawalAuditRequest(boolean approved,@NotBlank String reason) {}
