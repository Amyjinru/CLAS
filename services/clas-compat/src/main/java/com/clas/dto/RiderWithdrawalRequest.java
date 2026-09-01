package com.clas.dto;
import jakarta.validation.constraints.Min; import jakarta.validation.constraints.NotNull;
public record RiderWithdrawalRequest(@NotNull Long bankCardId,@NotNull @Min(1) Integer amount) {}
