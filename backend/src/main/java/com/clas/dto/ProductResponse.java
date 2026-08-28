package com.clas.dto;

import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String name,
    String description,
    Integer price,
    Integer stock,
    String status,
    String imageUrl,
    Long categoryId,
    String categoryName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
