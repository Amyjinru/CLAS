package com.clas.catalog.web;

import com.clas.catalog.api.ApiResponse;
import com.clas.catalog.api.AvailabilityRequest;
import com.clas.catalog.api.CatalogItem;
import com.clas.catalog.service.CatalogService;
import com.clas.catalog.service.InternalApiKeyVerifier;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/catalog/v1")
public class InternalCatalogController {
    private final CatalogService catalogService;
    private final InternalApiKeyVerifier internalApiKeyVerifier;

    public InternalCatalogController(CatalogService catalogService, InternalApiKeyVerifier internalApiKeyVerifier) {
        this.catalogService = catalogService;
        this.internalApiKeyVerifier = internalApiKeyVerifier;
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<CatalogItem> productSnapshot(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String internalApiKey,
            @RequestParam Long merchantId,
            @PathVariable Long productId) {
        internalApiKeyVerifier.verify(internalApiKey);
        return ApiResponse.ok(catalogService.productSnapshot(merchantId, productId));
    }

    @PostMapping("/products/availability")
    public ApiResponse<List<CatalogItem>> availability(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String internalApiKey,
            @Valid @RequestBody AvailabilityRequest request) {
        internalApiKeyVerifier.verify(internalApiKey);
        return ApiResponse.ok(catalogService.verifyAvailability(request));
    }
}
