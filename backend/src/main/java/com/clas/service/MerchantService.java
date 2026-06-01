package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.entity.Merchant;
import com.clas.mapper.MerchantMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MerchantService {
    private final MerchantMapper merchantMapper;

    public MerchantService(MerchantMapper merchantMapper) {
        this.merchantMapper = merchantMapper;
    }

    public List<Merchant> list() {
        return merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getStatus, "OPEN")
            .orderByDesc(Merchant::getScore));
    }

    public Merchant detail(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null || !"OPEN".equals(merchant.getStatus())) {
            throw new BusinessException("商家不存在或未营业");
        }
        return merchant;
    }
}

