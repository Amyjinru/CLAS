package com.clas.catalog.service;

import com.clas.catalog.api.AvailabilityRequest;
import com.clas.catalog.api.CatalogCategory;
import com.clas.catalog.api.CatalogItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    private final CatalogRepository catalogRepository;

    public CatalogService(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    public List<CatalogItem> listPublicProducts(Long merchantId) {
        return catalogRepository.listOnSaleByMerchant(merchantId);
    }

    public List<CatalogCategory> listCategories(Long merchantId) {
        return catalogRepository.listCategories(merchantId);
    }

    public CatalogItem productSnapshot(Long merchantId, Long productId) {
        return catalogRepository.findOnSaleById(productId, merchantId)
                .orElseThrow(() -> new CatalogNotFoundException("catalog product not found"));
    }

    public List<CatalogItem> verifyAvailability(AvailabilityRequest request) {
        List<CatalogItem> snapshots = new ArrayList<>();
        for (AvailabilityRequest.Item item : request.items()) {
            CatalogItem product = productSnapshot(request.merchantId(), item.productId());
            if (product.stock() < item.quantity()) {
                throw new InsufficientStockException("catalog product stock is insufficient: " + item.productId());
            }
            snapshots.add(product);
        }
        return snapshots;
    }
}
