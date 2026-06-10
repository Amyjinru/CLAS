package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clas.common.BusinessException;
import com.clas.dto.ProductCreateRequest;
import com.clas.dto.ProductResponse;
import com.clas.dto.ProductUpdateRequest;
import com.clas.entity.Product;
import com.clas.entity.ProductCategory;
import com.clas.mapper.ProductCategoryMapper;
import com.clas.mapper.ProductMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;

    public ProductService(ProductMapper productMapper, ProductCategoryMapper productCategoryMapper) {
        this.productMapper = productMapper;
        this.productCategoryMapper = productCategoryMapper;
    }

    public List<Product> listByMerchant(Long merchantId) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
            .eq(Product::getMerchantId, merchantId)
            .eq(Product::getStatus, "ON_SALE")
            .orderByAsc(Product::getId));
    }

    public Map<String, List<ProductResponse>> listGroupedByMerchant(Long merchantId) {
        List<ProductCategory> categories = listCategories(merchantId);
        Map<Long, String> categoryNames = categories.stream()
            .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
        Map<String, List<ProductResponse>> grouped = new LinkedHashMap<>();
        for (ProductCategory category : categories) {
            grouped.put(category.getName(), new java.util.ArrayList<>());
        }
        grouped.put("未分类", new java.util.ArrayList<>());

        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
            .eq(Product::getMerchantId, merchantId)
            .eq(Product::getStatus, "ON_SALE")
            .orderByAsc(Product::getCategoryId)
            .orderByAsc(Product::getId));
        for (Product product : products) {
            String categoryName = categoryNames.getOrDefault(product.getCategoryId(), "未分类");
            grouped.computeIfAbsent(categoryName, key -> new java.util.ArrayList<>())
                .add(toResponse(product, categoryName));
        }
        grouped.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return grouped;
    }

    public Page<Product> getMerchantProducts(Long merchantId, int page, int size, String keyword, Long categoryId) {
        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getMerchantId, merchantId)
                    .ne(Product::getStatus, "DELETED");
        if (categoryId != null) {
            if (categoryId == 0) {
                queryWrapper.isNull(Product::getCategoryId);
            } else {
                requireCategory(categoryId, merchantId);
                queryWrapper.eq(Product::getCategoryId, categoryId);
            }
        }
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

    public Page<Product> getMerchantProducts(Long merchantId, int page, int size, String keyword) {
        return getMerchantProducts(merchantId, page, size, keyword, null);
    }

    public Product createProduct(ProductCreateRequest request, Long merchantId) {
        Product product = new Product();
        product.setMerchantId(merchantId);
        product.setCategoryId(normalizeCategoryId(request.categoryId(), merchantId));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImage(request.imageUrl());
        product.setStatus("ON_SALE");
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
        product.setCategoryId(normalizeCategoryId(request.categoryId(), merchantId));
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

    public List<ProductCategory> listCategories(Long merchantId) {
        return productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
            .eq(ProductCategory::getMerchantId, merchantId)
            .orderByAsc(ProductCategory::getSortOrder)
            .orderByAsc(ProductCategory::getId));
    }

    public ProductCategory createCategory(Long merchantId, String name, Integer sortOrder) {
        String normalizedName = normalizeCategoryName(name);
        Long count = productCategoryMapper.selectCount(new LambdaQueryWrapper<ProductCategory>()
            .eq(ProductCategory::getMerchantId, merchantId)
            .eq(ProductCategory::getName, normalizedName));
        if (count > 0) {
            throw new BusinessException("分类名称已存在");
        }
        ProductCategory category = new ProductCategory();
        category.setMerchantId(merchantId);
        category.setName(normalizedName);
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        productCategoryMapper.insert(category);
        return category;
    }

    public ProductCategory updateCategory(Long merchantId, Long categoryId, String name, Integer sortOrder) {
        ProductCategory category = requireCategory(categoryId, merchantId);
        if (name != null) {
            String normalizedName = normalizeCategoryName(name);
            Long count = productCategoryMapper.selectCount(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getMerchantId, merchantId)
                .eq(ProductCategory::getName, normalizedName)
                .ne(ProductCategory::getId, categoryId));
            if (count > 0) {
                throw new BusinessException("分类名称已存在");
            }
            category.setName(normalizedName);
        }
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        productCategoryMapper.updateById(category);
        return category;
    }

    @Transactional
    public void deleteCategory(Long merchantId, Long categoryId) {
        requireCategory(categoryId, merchantId);
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
            .eq(Product::getMerchantId, merchantId)
            .eq(Product::getCategoryId, categoryId));
        for (Product product : products) {
            product.setCategoryId(null);
            productMapper.updateById(product);
        }
        productCategoryMapper.deleteById(categoryId);
    }

    public ProductResponse toResponse(Product product) {
        String categoryName = null;
        if (product.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
            if (category != null && Objects.equals(category.getMerchantId(), product.getMerchantId())) {
                categoryName = category.getName();
            }
        }
        return toResponse(product, categoryName);
    }

    private ProductResponse toResponse(Product product, String categoryName) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getStatus(),
            product.getImage(),
            product.getCategoryId(),
            categoryName,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    private ProductCategory requireCategory(Long categoryId, Long merchantId) {
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getMerchantId(), merchantId)) {
            throw new BusinessException("商品分类不存在或无权操作");
        }
        return category;
    }

    private Long normalizeCategoryId(Long categoryId, Long merchantId) {
        if (categoryId == null || categoryId == 0) {
            return null;
        }
        requireCategory(categoryId, merchantId);
        return categoryId;
    }

    private String normalizeCategoryName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("分类名称不能为空");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > 50) {
            throw new BusinessException("分类名称不能超过50个字符");
        }
        return normalizedName;
    }
}

