package com.clas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.Result;
import com.clas.dto.StockChangeRequest;
import com.clas.entity.GroupDeal;
import com.clas.entity.Product;
import com.clas.mapper.ProductMapper;
import com.clas.service.InternalCatalogProductService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/catalog/v1")
public class InternalCatalogController {
    private final InternalCatalogProductService internalCatalogProductService;
    private final ProductMapper productMapper;

    public InternalCatalogController(
        InternalCatalogProductService internalCatalogProductService,
        ProductMapper productMapper
    ) {
        this.internalCatalogProductService = internalCatalogProductService;
        this.productMapper = productMapper;
    }

    @GetMapping("/products/{productId}")
    public Result<Product> getProduct(@PathVariable Long productId) {
        return Result.ok(internalCatalogProductService.getProduct(productId));
    }

    @GetMapping("/products/batch")
    public Result<List<Product>> getProductsBatch(@RequestParam("ids") String ids) {
        return Result.ok(internalCatalogProductService.getProducts(internalCatalogProductService.parseIds(ids)));
    }

    @PostMapping("/products/{productId}/deduct-stock")
    public Result<Boolean> deductProductStock(@PathVariable Long productId, @RequestBody StockChangeRequest request) {
        return Result.ok(internalCatalogProductService.deductProductStock(
            productId, request == null ? null : request.quantity()
        ));
    }

    @PostMapping("/products/{productId}/restore-stock")
    public Result<Void> restoreProductStock(@PathVariable Long productId, @RequestBody StockChangeRequest request) {
        internalCatalogProductService.restoreProductStock(productId, request == null ? null : request.quantity());
        return Result.ok();
    }

    @GetMapping("/deals/{dealId}")
    public Result<GroupDeal> getDeal(@PathVariable Long dealId) {
        return Result.ok(internalCatalogProductService.getDeal(dealId));
    }

    @GetMapping("/deals/batch")
    public Result<List<GroupDeal>> getDealsBatch(@RequestParam("ids") String ids) {
        return Result.ok(internalCatalogProductService.getDeals(internalCatalogProductService.parseIds(ids)));
    }

    @PostMapping("/deals/{dealId}/deduct-stock")
    public Result<Boolean> deductDealStock(@PathVariable Long dealId) {
        return Result.ok(internalCatalogProductService.deductDealStock(dealId));
    }

    @PostMapping("/deals/{dealId}/restore-stock")
    public Result<Void> restoreDealStock(@PathVariable Long dealId) {
        internalCatalogProductService.restoreDealStock(dealId);
        return Result.ok();
    }

    @GetMapping("/merchants/product-average-prices")
    public Result<Map<Long, Integer>> productAveragePrices(@RequestParam("ids") String ids) {
        return Result.ok(internalCatalogProductService.computeProductAveragePrices(internalCatalogProductService.parseIds(ids)));
    }

    @GetMapping("/stats/public")
    public Result<Map<String, Long>> publicStats() {
        long products = productMapper.selectCount(new LambdaQueryWrapper<Product>()
            .eq(Product::getStatus, "ON_SALE"));
        return Result.ok(Map.of("products", products));
    }
}
