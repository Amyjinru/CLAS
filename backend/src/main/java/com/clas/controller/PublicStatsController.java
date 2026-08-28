package com.clas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.MerchantStatusEnum;
import com.clas.common.Result;
import com.clas.entity.Merchant;
import com.clas.entity.Product;
import com.clas.entity.User;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.ProductMapper;
import com.clas.mapper.UserMapper;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共统计端点 — 供落地页等无需认证的场景使用
 */
@RestController
@RequestMapping("/api/public")
public class PublicStatsController {

    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public PublicStatsController(MerchantMapper merchantMapper, ProductMapper productMapper, UserMapper userMapper) {
        this.merchantMapper = merchantMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        long merchants = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getStatus, MerchantStatusEnum.OPEN));
        long products = productMapper.selectCount(new LambdaQueryWrapper<Product>()
            .eq(Product::getStatus, "ON_SALE"));
        long users = userMapper.selectCount(null);
        return Result.ok(Map.of("merchants", merchants, "products", products, "users", users));
    }
}
