package com.clas.service;

import com.clas.client.CatalogClient;
import org.springframework.stereotype.Service;

@Service
public class MerchantContextService {
    private final CatalogClient catalogClient;

    public MerchantContextService(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    public Long getCurrentMerchantId() {
        return catalogClient.getCurrentMerchantId();
    }
}
