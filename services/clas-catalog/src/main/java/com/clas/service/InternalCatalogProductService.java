package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.entity.GroupDeal;
import com.clas.entity.Product;
import com.clas.mapper.GroupDealMapper;
import com.clas.mapper.ProductMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InternalCatalogProductService {
    private final ProductMapper productMapper;
    private final GroupDealMapper groupDealMapper;
    private final MerchantService merchantService;

    public InternalCatalogProductService(
        ProductMapper productMapper,
        GroupDealMapper groupDealMapper,
        MerchantService merchantService
    ) {
        this.productMapper = productMapper;
        this.groupDealMapper = groupDealMapper;
        this.merchantService = merchantService;
    }

    public Product getProduct(Long productId) {
        return productMapper.selectById(productId);
    }

    public List<Product> getProducts(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return productMapper.selectBatchIds(productIds);
    }

    public boolean deductProductStock(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("库存扣减数量无效");
        }
        return productMapper.deductStock(productId, quantity) > 0;
    }

    public void restoreProductStock(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return;
        }
        productMapper.restoreStock(productId, quantity);
    }

    public GroupDeal getDeal(Long dealId) {
        return groupDealMapper.selectById(dealId);
    }

    public List<GroupDeal> getDeals(List<Long> dealIds) {
        if (dealIds.isEmpty()) {
            return List.of();
        }
        return groupDealMapper.selectBatchIds(dealIds);
    }

    public boolean deductDealStock(Long dealId) {
        return groupDealMapper.deductStock(dealId) > 0;
    }

    public void restoreDealStock(Long dealId) {
        groupDealMapper.restoreStock(dealId);
    }

    public void refreshAveragePrice(Long merchantId) {
        merchantService.refreshAveragePrice(merchantId);
    }

    public Long getMerchantIdByUserId(String userId) {
        var merchant = merchantService.getMerchantByUserId(userId);
        return merchant == null ? null : merchant.id();
    }

    public List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .map(Long::valueOf)
            .toList();
    }
}
