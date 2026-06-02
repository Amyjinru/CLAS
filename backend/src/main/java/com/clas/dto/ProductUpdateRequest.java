package com.clas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductUpdateRequest(
    @NotNull(message = "商品ID不能为空")
    Long id,

    @NotBlank(message = "商品名称不能为空")
    String name,

    String description,

    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "价格不能小于0")
    Integer price,

    @NotNull(message = "商品库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    Integer stock,

    String imageUrl
) {
}
