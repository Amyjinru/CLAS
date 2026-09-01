package com.clas.dto;
import jakarta.validation.constraints.NotBlank;
public record RiderAuditRequest(@NotBlank String decision, String reason, Integer maxActiveOrders) {}
