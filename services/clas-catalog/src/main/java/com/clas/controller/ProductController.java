package com.clas.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.ProductCreateRequest;
import com.clas.dto.ProductUpdateRequest;
import com.clas.dto.ProductResponse;
import com.clas.dto.ProductListResponse;
import com.clas.entity.Product;
import com.clas.entity.ProductCategory;
import com.clas.client.MerchantClient;
import com.clas.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductController {
    private final ProductService productService;
    private final MerchantClient merchantClient;

    public ProductController(ProductService productService, MerchantClient merchantClient) {
        this.productService = productService;
        this.merchantClient = merchantClient;
    }

    @GetMapping("/api/product/list/{merchantId}")
    public Result<List<Product>> list(@PathVariable Long merchantId) {
        return Result.ok(productService.listByMerchant(merchantId));
    }

    @GetMapping("/api/product/list")
    public Result<Map<String, List<ProductResponse>>> listGrouped(@RequestParam Long merchantId) {
        return Result.ok(productService.listGroupedByMerchant(merchantId));
    }

    @GetMapping("/api/product/categories")
    public Result<List<ProductCategory>> listCategories(@RequestParam(required = false) Long merchantId) {
        Long resolvedMerchantId = merchantId == null ? merchantClient.getCurrentMerchantId() : merchantId;
        return Result.ok(productService.listCategories(resolvedMerchantId));
    }

    @GetMapping("/api/merchant/products/list")
    @Deprecated
    @RequireRole("MERCHANT")
    public Result<ProductListResponse> getMerchantProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return getMyProducts(page, size, keyword, categoryId);
    }

    @GetMapping("/api/merchant/me/products")
    @RequireRole("MERCHANT")
    public Result<ProductListResponse> getMyProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        Long merchantId = merchantClient.getCurrentMerchantId();
        Page<Product> productPage = productService.getMerchantProducts(merchantId, page, size, keyword, categoryId);

        List<ProductResponse> responses = productPage.getRecords().stream()
                .map(productService::toResponse).toList();

        return Result.ok(new ProductListResponse(
                responses,
                productPage.getTotal(),
                productPage.getCurrent(),
                productPage.getSize()
        ));
    }

    @PostMapping("/api/merchant/products/create")
    @Deprecated
    @RequireRole("MERCHANT")
    public Result<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return createMyProduct(request);
    }

    @PostMapping("/api/merchant/me/products")
    @RequireRole("MERCHANT")
    public Result<ProductResponse> createMyProduct(@Valid @RequestBody ProductCreateRequest request) {
        Long merchantId = merchantClient.getCurrentMerchantId();
        Product product = productService.createProduct(request, merchantId);
        return Result.ok(productService.toResponse(product));
    }

    @PutMapping("/api/merchant/products/update")
    @Deprecated
    @RequireRole("MERCHANT")
    public Result<ProductResponse> updateProduct(@Valid @RequestBody ProductUpdateRequest request) {
        return updateMyProduct(request);
    }

    @PutMapping("/api/merchant/me/products")
    @RequireRole("MERCHANT")
    public Result<ProductResponse> updateMyProduct(@Valid @RequestBody ProductUpdateRequest request) {
        Long merchantId = merchantClient.getCurrentMerchantId();
        Product product = productService.updateProduct(request, merchantId);
        return Result.ok(productService.toResponse(product));
    }

    public record CategoryRequest(Long id, String name, Integer sortOrder) {}

    @PostMapping("/api/product/categories")
    @RequireRole("MERCHANT")
    public Result<ProductCategory> createCategory(@Valid @RequestBody CategoryRequest request) {
        Long merchantId = merchantClient.getCurrentMerchantId();
        return Result.ok(productService.createCategory(merchantId, request.name(), request.sortOrder()));
    }

    @PutMapping("/api/product/categories")
    @RequireRole("MERCHANT")
    public Result<ProductCategory> updateCategory(@Valid @RequestBody CategoryRequest request) {
        Long merchantId = merchantClient.getCurrentMerchantId();
        return Result.ok(productService.updateCategory(merchantId, request.id(), request.name(), request.sortOrder()));
    }

    @DeleteMapping("/api/product/categories/{categoryId}")
    @RequireRole("MERCHANT")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        Long merchantId = merchantClient.getCurrentMerchantId();
        productService.deleteCategory(merchantId, categoryId);
        return Result.ok();
    }

    public record StatusUpdateRequest(Long productId, String status) {}

    @PatchMapping("/api/merchant/products/status")
    @Deprecated
    @RequireRole("MERCHANT")
    public Result<Void> updateStatus(@Valid @RequestBody StatusUpdateRequest request) {
        return updateMyProductStatus(request);
    }

    @PatchMapping("/api/merchant/me/products/status")
    @RequireRole("MERCHANT")
    public Result<Void> updateMyProductStatus(@Valid @RequestBody StatusUpdateRequest request) {
        Long merchantId = merchantClient.getCurrentMerchantId();
        productService.updateStatus(request.productId(), request.status(), merchantId);
        return Result.ok();
    }

    @DeleteMapping("/api/merchant/products/{productId}")
    @Deprecated
    @RequireRole("MERCHANT")
    public Result<Void> deleteProduct(@PathVariable Long productId) {
        return deleteMyProduct(productId);
    }

    @DeleteMapping("/api/merchant/me/products/{productId}")
    @RequireRole("MERCHANT")
    public Result<Void> deleteMyProduct(@PathVariable Long productId) {
        Long merchantId = merchantClient.getCurrentMerchantId();
        productService.deleteProduct(productId, merchantId);
        return Result.ok();
    }
}
