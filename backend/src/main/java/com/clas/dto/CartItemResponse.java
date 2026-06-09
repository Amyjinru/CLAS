package com.clas.dto;

public record CartItemResponse(
    Long id,
    String userId,
    Long productId,
    Long merchantId,
    String productName,
    Integer price,
    Integer stock,
    String image,
    Integer quantity,
    Integer subtotal,
    boolean valid,
    String invalidReason
) {
}
