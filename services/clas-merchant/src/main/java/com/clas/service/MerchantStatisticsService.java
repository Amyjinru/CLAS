package com.clas.service;

import com.clas.client.OrderClient;
import com.clas.dto.MerchantStatsDTO;
import org.springframework.stereotype.Service;

@Service
public class MerchantStatisticsService {
    private final OrderClient orderClient;

    public MerchantStatisticsService(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    public MerchantStatsDTO getMerchantStats(Long merchantId) {
        return orderClient.getMerchantStats(merchantId);
    }
}
