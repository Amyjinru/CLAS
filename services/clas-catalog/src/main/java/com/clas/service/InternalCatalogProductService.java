package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.entity.GroupDeal;
import com.clas.entity.Product;
import com.clas.mapper.GroupDealMapper;
import com.clas.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InternalCatalogProductService {
    private static final String PRODUCT_STATUS_DELETED = "DELETED";

    private final ProductMapper productMapper;
    private final GroupDealMapper groupDealMapper;

    public InternalCatalogProductService(ProductMapper productMapper, GroupDealMapper groupDealMapper) {
        this.productMapper = productMapper;
        this.groupDealMapper = groupDealMapper;
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

    public Map<Long, Integer> computeProductAveragePrices(List<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
            .in(Product::getMerchantId, merchantIds)
            .ne(Product::getStatus, PRODUCT_STATUS_DELETED));
        Map<Long, List<Integer>> pricesByMerchant = new HashMap<>();
        for (Product product : products) {
            if (product.getPrice() == null) {
                continue;
            }
            pricesByMerchant
                .computeIfAbsent(product.getMerchantId(), key -> new java.util.ArrayList<>())
                .add(product.getPrice());
        }
        Map<Long, Integer> averages = new HashMap<>();
        for (Map.Entry<Long, List<Integer>> entry : pricesByMerchant.entrySet()) {
            averages.put(entry.getKey(), averageInt(entry.getValue()));
        }
        return averages;
    }

    public List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(Long::valueOf)
                .toList();
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, "商品 ID 必须为整数", "VALIDATION_ERROR");
        }
    }

    private int averageInt(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (Integer value : values) {
            total += value == null ? 0 : value;
        }
        return (int) Math.round((double) total / values.size());
    }
}
