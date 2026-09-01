package com.clas.service;

import com.clas.client.MerchantClient;
import org.springframework.stereotype.Service;

@Service
public class MerchantContextService {
    private final MerchantClient merchantClient;

    public MerchantContextService(MerchantClient merchantClient) {
        this.merchantClient = merchantClient;
    }

    public Long getCurrentMerchantId() {
        return merchantClient.getCurrentMerchantId();
    }
}
