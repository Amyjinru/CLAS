package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.config.UserContext;
import com.clas.dto.DealRequest;
import com.clas.entity.DealOrder;
import com.clas.entity.GroupDeal;
import com.clas.mapper.DealOrderMapper;
import com.clas.mapper.GroupDealMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DealService {
    private final GroupDealMapper groupDealMapper;
    private final DealOrderMapper dealOrderMapper;
    private final MerchantService merchantService;
    private final NotificationService notificationService;

    public DealService(GroupDealMapper groupDealMapper, DealOrderMapper dealOrderMapper, MerchantService merchantService, NotificationService notificationService) {
        this.groupDealMapper = groupDealMapper;
        this.dealOrderMapper = dealOrderMapper;
        this.merchantService = merchantService;
        this.notificationService = notificationService;
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

    @Transactional
    public DealOrder buy(Long dealId) {
        GroupDeal deal = groupDealMapper.selectById(dealId);
        if (deal == null || !"ON_SALE".equals(deal.getStatus())) {
            throw new BusinessException("团购券不存在或已下架");
        }
        if (deal.getStock() <= 0) {
            throw new BusinessException("团购券库存不足");
        }
        deal.setStock(deal.getStock() - 1);
        groupDealMapper.updateById(deal);

        DealOrder order = new DealOrder();
        order.setDealId(deal.getId());
        order.setUserId(UserContext.getUserId());
        order.setMerchantId(deal.getMerchantId());
        order.setVoucherCode("CLAS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus("UNUSED");
        order.setPayAmount(deal.getDealPrice());
        order.setCreateTime(LocalDateTime.now());
        dealOrderMapper.insert(order);
        notificationService.send(order.getUserId(), "团购券购买成功", "券码 " + order.getVoucherCode() + " 已生成，可到店核销。");
        return order;
    }

    public List<DealOrder> myOrders() {
        return dealOrderMapper.selectList(new LambdaQueryWrapper<DealOrder>()
            .eq(DealOrder::getUserId, UserContext.getUserId())
            .orderByDesc(DealOrder::getId));
    }

    @Transactional
    public DealOrder redeem(String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new BusinessException("券码不能为空");
        }
        DealOrder order = dealOrderMapper.selectOne(new LambdaQueryWrapper<DealOrder>()
            .eq(DealOrder::getVoucherCode, voucherCode.trim()));
        if (order == null) {
            throw new BusinessException("券码不存在");
        }
        if (!merchantService.getCurrentMerchantId().equals(order.getMerchantId())) {
            throw new BusinessException("只能核销自己店铺的团购券");
        }
        if (!"UNUSED".equals(order.getStatus())) {
            throw new BusinessException("该团购券已核销或已失效");
        }
        order.setStatus("USED");
        order.setUsedTime(LocalDateTime.now());
        dealOrderMapper.updateById(order);
        notificationService.send(order.getUserId(), "团购券已核销", "券码 " + order.getVoucherCode() + " 已成功核销。");
        return order;
    }
}
