package com.clas.catalog.api;

public record CatalogCategory(Long id, Long merchantId, String name, Integer sortOrder) {
}
