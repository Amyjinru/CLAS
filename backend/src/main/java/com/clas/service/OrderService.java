package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.CreateOrderRequest;
import com.clas.dto.OrderResponse;
import com.clas.entity.Cart;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.entity.Product;
import com.clas.mapper.CartMapper;
import com.clas.mapper.OrderItemMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ProductMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    public OrderService(
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        CartMapper cartMapper,
        ProductMapper productMapper
    ) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        List<Cart> cartItems = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, request.userId()));
        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车为空");
        }
        List<Long> productIds = cartItems.stream().map(Cart::getProductId).toList();
        Map<Long, Product> products = productMapper.selectBatchIds(productIds).stream()
            .filter(product -> request.merchantId().equals(product.getMerchantId()))
            .collect(Collectors.toMap(Product::getId, product -> product));
        List<Cart> merchantCartItems = cartItems.stream()
            .filter(item -> products.containsKey(item.getProductId()))
            .toList();
        if (merchantCartItems.isEmpty()) {
            throw new BusinessException("该商家的购物车为空");
        }

        int totalPrice = 0;
        for (Cart item : merchantCartItems) {
            Product product = products.get(item.getProductId());
            if (!"ON_SALE".equals(product.getStatus())) {
                throw new BusinessException("商品已下架：" + product.getName());
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException("库存不足：" + product.getName());
            }
            totalPrice += product.getPrice() * item.getQuantity();
        }

        Orders order = new Orders();
        order.setUserId(request.userId());
        order.setMerchantId(request.merchantId());
        order.setTotalPrice(totalPrice);
        order.setStatus("PENDING_PAYMENT");
        order.setCreateTime(LocalDateTime.now());
        ordersMapper.insert(order);

        List<OrderItem> orderItems = merchantCartItems.stream().map(cart -> {
            Product product = products.get(cart.getProductId());
            product.setStock(product.getStock() - cart.getQuantity());
            productMapper.updateById(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItemMapper.insert(orderItem);
            cartMapper.deleteById(cart.getId());
            return orderItem;
        }).toList();

        return new OrderResponse(order, orderItems);
    }

    public List<OrderResponse> listForUser(Long userId) {
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getUserId, userId)
            .orderByDesc(Orders::getCreateTime));
        return withItems(orders);
    }

    public List<OrderResponse> listForMerchant(Long merchantId) {
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getMerchantId, merchantId)
            .orderByDesc(Orders::getCreateTime));
        return withItems(orders);
    }

    public Orders pay(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, "PENDING_PAYMENT");
        order.setStatus("PAID");
        ordersMapper.updateById(order);
        return order;
    }

    public Orders accept(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, "PAID");
        order.setStatus("ACCEPTED");
        ordersMapper.updateById(order);
        return order;
    }

    public Orders complete(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, "ACCEPTED");
        order.setStatus("COMPLETED");
        ordersMapper.updateById(order);
        return order;
    }

    public Orders requireOrder(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private List<OrderResponse> withItems(List<Orders> orders) {
        return orders.stream()
            .map(order -> new OrderResponse(order, orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()))))
            .toList();
    }

    private void requireStatus(Orders order, String status) {
        if (!status.equals(order.getStatus())) {
            throw new BusinessException("订单状态错误，当前状态：" + order.getStatus());
        }
    }
}

