package com.clas.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.ProductCreateRequest;
import com.clas.dto.ProductUpdateRequest;
import com.clas.dto.ProductResponse;
import com.clas.dto.ProductListResponse;
import com.clas.entity.Product;
import com.clas.service.MerchantService;
import com.clas.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductController {
    private final ProductService productService;
    private final MerchantService merchantService;

    public ProductController(ProductService productService, MerchantService merchantService) {
        this.productService = productService;
        this.merchantService = merchantService;
    }

    @GetMapping("/api/product/list/{merchantId}")
    public Result<List<Product>> list(@PathVariable Long merchantId) {
        return Result.ok(productService.listByMerchant(merchantId));
    }

    @GetMapping("/api/merchant/products/list")
    @RequireRole("MERCHANT")
    public Result<ProductListResponse> getMerchantProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Long merchantId = merchantService.getCurrentMerchantId();
        Page<Product> productPage = productService.getMerchantProducts(merchantId, page, size, keyword);

        List<ProductResponse> responses = productPage.getRecords().stream()
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getPrice(),
                        p.getStock(),
                        p.getStatus(),
                        p.getImage(),
                        p.getCreatedAt(),
                        p.getUpdatedAt()
                )).toList();

        return Result.ok(new ProductListResponse(
                responses,
                productPage.getTotal(),
                productPage.getCurrent(),
                productPage.getSize()
        ));
    }

    @PostMapping("/api/merchant/products/create")
    @RequireRole("MERCHANT")
    public Result<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Long merchantId = merchantService.getCurrentMerchantId();
        Product product = productService.createProduct(request, merchantId);
        return Result.ok(new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getImage(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        ));
    }

    @PutMapping("/api/merchant/products/update")
    @RequireRole("MERCHANT")
    public Result<ProductResponse> updateProduct(@Valid @RequestBody ProductUpdateRequest request) {
        Long merchantId = merchantService.getCurrentMerchantId();
        Product product = productService.updateProduct(request, merchantId);
        return Result.ok(new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getImage(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        ));
    }

    public record StatusUpdateRequest(Long productId, String status) {}

    @PatchMapping("/api/merchant/products/status")
    @RequireRole("MERCHANT")
    public Result<Void> updateStatus(@Valid @RequestBody StatusUpdateRequest request) {
        Long merchantId = merchantService.getCurrentMerchantId();
        productService.updateStatus(request.productId(), request.status(), merchantId);
        return Result.ok();
    }

    @DeleteMapping("/api/merchant/products/{productId}")
    @RequireRole("MERCHANT")
    public Result<Void> deleteProduct(@PathVariable Long productId) {
        Long merchantId = merchantService.getCurrentMerchantId();
        productService.deleteProduct(productId, merchantId);
        return Result.ok();
    }
}

