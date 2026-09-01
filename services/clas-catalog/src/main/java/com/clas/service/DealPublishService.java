package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.DealRequest;
import com.clas.entity.GroupDeal;
import com.clas.mapper.GroupDealMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DealPublishService {
    private final GroupDealMapper groupDealMapper;
    private final MerchantService merchantService;

    public DealPublishService(GroupDealMapper groupDealMapper, MerchantService merchantService) {
        this.groupDealMapper = groupDealMapper;
        this.merchantService = merchantService;
    }

    public List<GroupDeal> list(Long merchantId) {
        LambdaQueryWrapper<GroupDeal> wrapper = new LambdaQueryWrapper<GroupDeal>()
            .eq(GroupDeal::getStatus, "ON_SALE")
            .orderByDesc(GroupDeal::getId);
        if (merchantId != null) {
            wrapper.eq(GroupDeal::getMerchantId, merchantId);
        }
        return groupDealMapper.selectList(wrapper);
    }

    public GroupDeal getById(Long dealId) {
        GroupDeal deal = groupDealMapper.selectById(dealId);
        if (deal == null) {
            throw new BusinessException("团购券不存在");
        }
        return deal;
    }

    public List<GroupDeal> merchantDeals() {
        return groupDealMapper.selectList(new LambdaQueryWrapper<GroupDeal>()
            .eq(GroupDeal::getMerchantId, merchantService.getCurrentMerchantId())
            .orderByDesc(GroupDeal::getId));
    }

    public GroupDeal create(DealRequest request) {
        String status = request.status() == null || request.status().isBlank() ? "ON_SALE" : request.status();
        if (!"ON_SALE".equals(status) && !"OFF_SALE".equals(status)) {
            throw new BusinessException("团购状态只能是 ON_SALE 或 OFF_SALE");
        }
        GroupDeal deal = new GroupDeal();
        deal.setMerchantId(merchantService.getCurrentMerchantId());
        deal.setTitle(request.title());
        deal.setDescription(request.description());
        deal.setOriginalPrice(request.originalPrice());
        deal.setDealPrice(request.dealPrice());
        deal.setStock(request.stock());
        deal.setValidDays(request.validDays());
        deal.setStatus(status);
        groupDealMapper.insert(deal);
        return deal;
    }

    public GroupDeal updateMerchantDeal(Long dealId, DealRequest request) {
        GroupDeal deal = groupDealMapper.selectById(dealId);
        if (deal == null) {
            throw new BusinessException("团购券不存在");
        }
        Long merchantId = merchantService.getCurrentMerchantId();
        if (!merchantId.equals(deal.getMerchantId())) {
            throw new BusinessException("只能修改自己店铺的团购");
        }
        String status = request.status() == null || request.status().isBlank() ? "ON_SALE" : request.status();
        if (!"ON_SALE".equals(status) && !"OFF_SALE".equals(status)) {
            throw new BusinessException("团购状态只能是 ON_SALE 或 OFF_SALE");
        }
        deal.setTitle(request.title());
        deal.setDescription(request.description());
        deal.setOriginalPrice(request.originalPrice());
        deal.setDealPrice(request.dealPrice());
        deal.setStock(request.stock());
        deal.setValidDays(request.validDays());
        deal.setStatus(status);
        groupDealMapper.updateById(deal);
        return deal;
    }
}
