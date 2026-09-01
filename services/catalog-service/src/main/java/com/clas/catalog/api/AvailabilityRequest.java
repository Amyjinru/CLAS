package com.clas.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AvailabilityRequest(
        @NotNull Long merchantId,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {
    }
}
