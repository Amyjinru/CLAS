package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.MerchantStatusEnum;
import com.clas.dto.RoleApplicationRecordResponse;
import com.clas.entity.Merchant;
import com.clas.entity.MerchantAuditLog;
import com.clas.mapper.MerchantAuditLogMapper;
import com.clas.mapper.MerchantMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InternalMerchantService {
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final MerchantMapper merchantMapper;
    private final MerchantAuditLogMapper merchantAuditLogMapper;

    public InternalMerchantService(MerchantMapper merchantMapper, MerchantAuditLogMapper merchantAuditLogMapper) {
        this.merchantMapper = merchantMapper;
        this.merchantAuditLogMapper = merchantAuditLogMapper;
    }

    public Merchant getById(Long merchantId) {
        return merchantMapper.selectById(merchantId);
    }

    public List<Merchant> getByIds(List<Long> merchantIds) {
        if (merchantIds.isEmpty()) {
            return List.of();
        }
        return merchantMapper.selectBatchIds(merchantIds);
    }

    public boolean hasPendingApplication(String userId) {
        return merchantMapper.exists(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getUserId, userId)
            .eq(Merchant::getStatus, MerchantStatusEnum.PENDING));
    }

    public RoleApplicationRecordResponse getApplicationRecord(String userId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getUserId, userId));
        if (merchant == null) {
            return null;
        }
        MerchantAuditLog latestAudit = merchantAuditLogMapper.selectOne(new LambdaQueryWrapper<MerchantAuditLog>()
            .eq(MerchantAuditLog::getMerchantId, merchant.getId())
            .orderByDesc(MerchantAuditLog::getId)
            .last("LIMIT 1"));
        return new RoleApplicationRecordResponse(
            "merchant-" + merchant.getId(),
            "MERCHANT",
            merchantApplicationStatus(merchant.getStatus() == null ? null : merchant.getStatus().name()),
            merchant.getMerchantName() + " 的入驻申请",
            merchant.getAdminRemarks() != null ? merchant.getAdminRemarks() : latestAudit == null ? null : latestAudit.getRemarks(),
            latestAudit == null ? null : latestAudit.getAdminId(),
            merchant.getCreatedAt(),
            merchant.getUpdatedAt()
        );
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

    public void updateScore(Long merchantId, BigDecimal score) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        merchant.setScore(score);
        merchantMapper.updateById(merchant);
    }

    private String merchantApplicationStatus(String status) {
        if ("OPEN".equals(status) || "CLOSED".equals(status) || "APPROVED".equals(status)) {
            return APPROVED;
        }
        if ("BLOCKED".equals(status) || "DISABLED".equals(status)) {
            return REJECTED;
        }
        return PENDING;
    }
}
