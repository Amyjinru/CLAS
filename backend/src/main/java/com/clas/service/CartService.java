package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.AddCartRequest;
import com.clas.dto.CartItemResponse;
import com.clas.entity.Cart;
import com.clas.entity.Product;
import com.clas.mapper.CartMapper;
import com.clas.mapper.ProductMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CartService {
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    public CartService(CartMapper cartMapper, ProductMapper productMapper) {
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
    }

    public List<CartItemResponse> add(AddCartRequest request) {
        Product product = productMapper.selectById(request.productId());
        if (product == null || !"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (product.getStock() < request.quantity()) {
            throw new BusinessException("库存不足");
        }
        Cart cart = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, request.userId())
            .eq(Cart::getProductId, request.productId()));
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(request.userId());
            cart.setProductId(request.productId());
            cart.setQuantity(request.quantity());
            cartMapper.insert(cart);
        } else {
            int nextQuantity = cart.getQuantity() + request.quantity();
            if (nextQuantity > product.getStock()) {
                throw new BusinessException("库存不足");
            }
            cart.setQuantity(nextQuantity);
            cartMapper.updateById(cart);
        }
        return list(request.userId());
    }

    public List<CartItemResponse> list(Long userId) {
        List<Cart> cartItems = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, userId)
            .orderByAsc(Cart::getId));
        List<Long> productIds = cartItems.stream().map(Cart::getProductId).toList();
        Map<Long, Product> products = productIds.isEmpty()
            ? Map.of()
            : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        return cartItems.stream()
            .map(item -> toResponse(item, products.get(item.getProductId())))
            .toList();
    }

    public void clear(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    private CartItemResponse toResponse(Cart item, Product product) {
        String name = product == null ? "商品已失效" : product.getName();
        Integer price = product == null ? 0 : product.getPrice();
        Long merchantId = product == null ? null : product.getMerchantId();
        Integer stock = product == null ? 0 : product.getStock();
        String image = product == null ? null : product.getImage();
        return new CartItemResponse(
            item.getId(),
            item.getUserId(),
            item.getProductId(),
            merchantId,
            name,
            price,
            stock,
            image,
            item.getQuantity(),
            price * item.getQuantity()
        );
    }
}

