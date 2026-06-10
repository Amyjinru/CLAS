package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
    Long orderId,
    Long merchantId,
    String userId,
    @NotBlank String content
) {}
