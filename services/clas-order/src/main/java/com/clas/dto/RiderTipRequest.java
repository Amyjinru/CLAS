package com.clas.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
public record RiderTipRequest(@Min(1) @Max(5000) Integer amount, @NotBlank String idempotencyKey) {}
