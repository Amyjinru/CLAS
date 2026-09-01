package com.clas.catalog.api;

public record CatalogItem(
        Long id,
        Long merchantId,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        Integer price,
        Integer stock,
        String status,
        String image
) {
}
