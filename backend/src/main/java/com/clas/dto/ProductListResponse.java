package com.clas.dto;

import java.util.List;

public record ProductListResponse(
    List<ProductResponse> list,
    long total,
    long page,
    long size
) {
}
