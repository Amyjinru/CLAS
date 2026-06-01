package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.entity.Product;
import com.clas.mapper.ProductMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public List<Product> listByMerchant(Long merchantId) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
            .eq(Product::getMerchantId, merchantId)
            .eq(Product::getStatus, "ON_SALE")
            .orderByAsc(Product::getId));
    }
}

