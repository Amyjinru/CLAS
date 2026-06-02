package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clas.common.BusinessException;
import com.clas.dto.ProductCreateRequest;
import com.clas.dto.ProductUpdateRequest;
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

    public Page<Product> getMerchantProducts(Long merchantId, int page, int size, String keyword) {
        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getMerchantId, merchantId)
                    .ne(Product::getStatus, "DELETED");
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper
                .like(Product::getName, keyword)
                .or()
                .like(Product::getDescription, keyword)
            );
        }
        queryWrapper.orderByDesc(Product::getId);
        return productMapper.selectPage(pageParam, queryWrapper);
    }

    public Product createProduct(ProductCreateRequest request, Long merchantId) {
        Product product = new Product();
        product.setMerchantId(merchantId);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImage(request.imageUrl());
        product.setStatus("OFF_SALE");
        productMapper.insert(product);
        return product;
    }

    public Product updateProduct(ProductUpdateRequest request, Long merchantId) {
        Product product = productMapper.selectById(request.id());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作此商品");
        }

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.stock() != null) {
            product.setStock(request.stock());
        }
        if (request.imageUrl() != null) {
            product.setImage(request.imageUrl());
        }

        productMapper.updateById(product);
        return product;
    }

    public void updateStatus(Long productId, String newStatus, Long merchantId) {
        if (!"ON_SALE".equals(newStatus) && !"OFF_SALE".equals(newStatus)) {
            throw new BusinessException("非法商品状态");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作此商品");
        }
        product.setStatus(newStatus);
        productMapper.updateById(product);
    }

    public void deleteProduct(Long productId, Long merchantId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作此商品");
        }
        product.setStatus("DELETED");
        productMapper.updateById(product);
    }
}

