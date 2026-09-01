package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.CatalogClient;
import com.clas.client.MerchantClient;
import com.clas.client.IamClient;
import com.clas.common.BusinessException;
import com.clas.dto.AddCartRequest;
import com.clas.dto.CartItemResponse;
import com.clas.dto.CartValidationResponse;
import com.clas.dto.RemoveCartRequest;
import com.clas.dto.UpdateCartRequest;
import com.clas.entity.Cart;
import com.clas.entity.Merchant;
import com.clas.entity.Product;
import com.clas.mapper.CartMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {
    private final CartMapper cartMapper;
    private final CatalogClient catalogClient;
    private final MerchantClient merchantClient;
    private final IamClient iamClient;

    public CartService(CartMapper cartMapper, CatalogClient catalogClient, MerchantClient merchantClient, IamClient iamClient) {
        this.cartMapper = cartMapper;
        this.catalogClient = catalogClient;
        this.merchantClient = merchantClient;
        this.iamClient = iamClient;
    }

    public List<CartItemResponse> add(AddCartRequest request) {
        iamClient.assertCanUsePlatform(request.userId());
        Product product = catalogClient.getProduct(request.productId());
        if (product == null || !"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("?????????");
        }
        if (product.getStock() < request.quantity()) {
            throw new BusinessException("????");
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
                throw new BusinessException("????");
            }
            cart.setQuantity(nextQuantity);
            cartMapper.updateById(cart);
        }
        return list(request.userId());
    }

    public List<CartItemResponse> remove(RemoveCartRequest request) {
        Cart cart = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, request.userId())
            .eq(Cart::getProductId, request.productId()));
        if (cart == null) {
            throw new BusinessException("?????????");
        }
        int nextQuantity = cart.getQuantity() - request.quantity();
        if (nextQuantity <= 0) {
            cartMapper.deleteById(cart.getId());
        } else {
            cart.setQuantity(nextQuantity);
            cartMapper.updateById(cart);
        }
        return list(request.userId());
    }

    public List<CartItemResponse> update(UpdateCartRequest request) {
        Cart cart = requireCartItem(request.userId(), request.productId());
        Product product = catalogClient.getProduct(request.productId());
        if (product == null || !"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("?????????");
        }
        if (product.getStock() < request.quantity()) {
            throw new BusinessException("????");
        }
        cart.setQuantity(request.quantity());
        cartMapper.updateById(cart);
        return list(request.userId());
    }

    public List<CartItemResponse> deleteItem(String userId, Long productId) {
        Cart cart = requireCartItem(userId, productId);
        cartMapper.deleteById(cart.getId());
        return list(userId);
    }

    public List<CartItemResponse> list(String userId) {
        return validate(userId).items();
    }

    public CartValidationResponse validate(String userId) {
        List<Cart> cartItems = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, userId)
            .orderByAsc(Cart::getId));
        List<Long> productIds = cartItems.stream().map(Cart::getProductId).toList();
        Map<Long, Product> products = productIds.isEmpty()
            ? Map.of()
            : catalogClient.getProducts(productIds);
        List<Long> merchantIdList = products.values().stream()
            .map(Product::getMerchantId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        Map<Long, Merchant> merchants = merchantIdList.isEmpty()
            ? Map.of()
            : merchantClient.getMerchants(merchantIdList);
        List<CartItemResponse> items = cartItems.stream()
            .map(item -> {
                Product product = products.get(item.getProductId());
                Merchant merchant = product == null ? null : merchants.get(product.getMerchantId());
                return toResponse(item, product, merchant);
            })
            .toList();
        int invalidCount = (int) items.stream().filter(item -> !item.valid()).count();
        Set<Long> merchantIds = items.stream()
            .map(CartItemResponse::merchantId)
            .filter(id -> id != null)
            .collect(Collectors.toCollection(HashSet::new));
        return new CartValidationResponse(items, invalidCount, merchantIds.size() > 1);
    }

    public List<CartItemResponse> clearInvalid(String userId) {
        CartValidationResponse validation = validate(userId);
        for (CartItemResponse item : validation.items()) {
            if (!item.valid()) {
                cartMapper.deleteById(item.id());
            }
        }
        return list(userId);
    }

    public void clear(String userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    private Cart requireCartItem(String userId, Long productId) {
        Cart cart = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, userId)
            .eq(Cart::getProductId, productId));
        if (cart == null) {
            throw new BusinessException("?????????");
        }
        return cart;
    }

    private CartItemResponse toResponse(Cart item, Product product, Merchant merchant) {
        String invalidReason = resolveInvalidReason(item, product);
        boolean valid = invalidReason == null;
        String name = product == null ? "?????" : product.getName();
        Integer price = product == null ? 0 : product.getPrice();
        Long merchantId = product == null ? null : product.getMerchantId();
        Integer stock = product == null ? 0 : product.getStock();
        String image = product == null ? null : product.getImage();
        return new CartItemResponse(
            item.getId(),
            item.getUserId(),
            item.getProductId(),
            merchantId,
            merchant == null ? null : merchant.getMerchantName(),
            name,
            price,
            stock,
            image,
            item.getQuantity(),
            price * item.getQuantity(),
            valid,
            invalidReason
        );
    }

    private String resolveInvalidReason(Cart item, Product product) {
        if (product == null) {
            return "?????";
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            return "?????";
        }
        if (product.getStock() <= 0) {
            return "?????";
        }
        if (item.getQuantity() > product.getStock()) {
            return "????";
        }
        return null;
    }
}
