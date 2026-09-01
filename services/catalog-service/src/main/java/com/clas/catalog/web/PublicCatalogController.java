package com.clas.catalog.web;

import com.clas.catalog.api.ApiResponse;
import com.clas.catalog.api.CatalogCategory;
import com.clas.catalog.api.CatalogItem;
import com.clas.catalog.service.CatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class PublicCatalogController {
    private final CatalogService catalogService;

    public PublicCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/list/{merchantId}")
    public ApiResponse<List<CatalogItem>> list(@PathVariable Long merchantId) {
        return ApiResponse.ok(catalogService.listPublicProducts(merchantId));
    }

    @GetMapping("/list")
    public ApiResponse<List<CatalogItem>> listByQuery(@RequestParam Long merchantId) {
        return ApiResponse.ok(catalogService.listPublicProducts(merchantId));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CatalogCategory>> categories(@RequestParam Long merchantId) {
        return ApiResponse.ok(catalogService.listCategories(merchantId));
    }
}
