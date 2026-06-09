package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReviewRequest(
    @jakarta.validation.constraints.NotNull Long orderId,
    String userId,
    @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) Integer score,
    String content,
    @Size(max = 9) List<String> images
) {
}
