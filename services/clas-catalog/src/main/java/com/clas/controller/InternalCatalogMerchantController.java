package com.clas.controller;

import com.clas.common.BusinessException;
import com.clas.common.Result;
import com.clas.dto.MerchantScoreUpdateRequest;
import com.clas.dto.RoleApplicationRecordResponse;
import com.clas.dto.StockChangeRequest;
import com.clas.entity.GroupDeal;
import com.clas.entity.Merchant;
import com.clas.entity.Product;
import com.clas.service.InternalCatalogProductService;
import com.clas.service.InternalMerchantService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/catalog/v1")
public class InternalCatalogMerchantController {
    private final InternalMerchantService internalMerchantService;
    private final InternalCatalogProductService internalCatalogProductService;

    public InternalCatalogMerchantController(
        InternalMerchantService internalMerchantService,
        InternalCatalogProductService internalCatalogProductService
    ) {
        this.internalMerchantService = internalMerchantService;
        this.internalCatalogProductService = internalCatalogProductService;
    }

    @GetMapping("/merchants/{merchantId}")
    public Result<Merchant> getMerchant(@PathVariable Long merchantId) {
        return Result.ok(internalMerchantService.getById(merchantId));
    }

    @GetMapping("/merchants/batch")
    public Result<List<Merchant>> getMerchantsBatch(@RequestParam("ids") String ids) {
        return Result.ok(internalMerchantService.getByIds(internalMerchantService.parseIds(ids)));
    }

    @GetMapping("/users/{userId}/merchant-pending")
    public Result<Boolean> hasPendingMerchantApplication(@PathVariable String userId) {
        return Result.ok(internalMerchantService.hasPendingApplication(userId));
    }

    @GetMapping("/users/{userId}/merchant-application-record")
    public Result<RoleApplicationRecordResponse> getMerchantApplicationRecord(@PathVariable String userId) {
        return Result.ok(internalMerchantService.getApplicationRecord(userId));
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
        return Result.ok(internalCatalogProductService.deductProductStock(productId, request.quantity()));
    }

    @PostMapping("/products/{productId}/restore-stock")
    public Result<Void> restoreProductStock(@PathVariable Long productId, @RequestBody StockChangeRequest request) {
        internalCatalogProductService.restoreProductStock(productId, request.quantity());
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

    @GetMapping("/users/{userId}/merchant-id")
    public Result<Long> getMerchantIdByUser(@PathVariable String userId) {
        return Result.ok(internalCatalogProductService.getMerchantIdByUserId(userId));
    }

    @PostMapping("/merchants/{merchantId}/refresh-average-price")
    public Result<Void> refreshAveragePrice(@PathVariable Long merchantId) {
        internalCatalogProductService.refreshAveragePrice(merchantId);
        return Result.ok();
    }

    @PostMapping("/merchants/{merchantId}/score")
    public Result<Void> updateMerchantScore(@PathVariable Long merchantId, @RequestBody MerchantScoreUpdateRequest request) {
        internalMerchantService.updateScore(merchantId, request.score());
        return Result.ok();
    }
}
