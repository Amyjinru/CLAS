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
    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELED = "CANCELED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_REFUND_PENDING = "REFUND_PENDING";

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;

    public OrderService(
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        CartMapper cartMapper,
        ProductMapper productMapper,
        NotificationService notificationService
    ) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.notificationService = notificationService;
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
        order.setStatus(STATUS_PENDING_PAYMENT);
        order.setDeliveryAddress(request.deliveryAddress());
        order.setDeliveryStatus("WAITING");
        order.setEstimatedMinutes(30);
        order.setRefundStatus("NONE");
        order.setCreateTime(LocalDateTime.now());
        ordersMapper.insert(order);

        List<OrderItem> orderItems = merchantCartItems.stream().map(cart -> {
            Product product = products.get(cart.getProductId());
            int rows = productMapper.deductStock(product.getId(), cart.getQuantity());
            if (rows == 0) {
                throw new BusinessException("库存不足：" + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItemMapper.insert(orderItem);
            cartMapper.deleteById(cart.getId());
            return orderItem;
        }).toList();

        notificationService.send(order.getUserId(), "订单已创建", "订单 " + order.getId() + " 已创建，请及时完成支付。");
        return new OrderResponse(order, orderItems);
    }

    public List<OrderResponse> listForUser(String userId) {
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

    public Orders accept(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, STATUS_PAID);
        order.setStatus(STATUS_ACCEPTED);
        ordersMapper.updateById(order);
        return order;
    }

    public Orders accept(Long orderId, Long merchantId) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatus(order, STATUS_PAID);
        order.setStatus(STATUS_ACCEPTED);
        order.setDeliveryStatus("PREPARING");
        ordersMapper.updateById(order);
        notificationService.send(order.getUserId(), "商家已接单", "订单 " + order.getId() + " 正在备餐。");
        return order;
    }

    public Orders deliver(Long orderId, Long merchantId) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatus(order, STATUS_ACCEPTED);
        order.setDeliveryStatus("DELIVERING");
        order.setEstimatedMinutes(15);
        ordersMapper.updateById(order);
        notificationService.send(order.getUserId(), "订单配送中", "订单 " + order.getId() + " 已进入配送流程。");
        return order;
    }

    public Orders complete(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, STATUS_ACCEPTED);
        order.setStatus(STATUS_COMPLETED);
        ordersMapper.updateById(order);
        return order;
    }

    public Orders complete(Long orderId, String userId) {
        Orders order = requireUserOrder(orderId, userId);
        requireStatus(order, STATUS_ACCEPTED);
        order.setStatus(STATUS_COMPLETED);
        order.setDeliveryStatus("DELIVERED");
        ordersMapper.updateById(order);
        notificationService.send(order.getUserId(), "订单已完成", "订单 " + order.getId() + " 已完成，欢迎评价本次体验。");
        return order;
    }

    @Transactional
    public Orders cancel(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatusIn(order, STATUS_PENDING_PAYMENT, STATUS_PAID);
        restoreOrderStock(orderId);
        order.setStatus(STATUS_CANCELED);
        ordersMapper.updateById(order);
        return order;
    }

    @Transactional
    public Orders cancel(Long orderId, String userId) {
        Orders order = requireUserOrder(orderId, userId);
        requireStatusIn(order, STATUS_PENDING_PAYMENT, STATUS_PAID);
        restoreOrderStock(orderId);
        order.setStatus(STATUS_CANCELED);
        ordersMapper.updateById(order);
        return order;
    }

    @Transactional
    public Orders reject(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, STATUS_PAID);
        restoreOrderStock(orderId);
        order.setStatus(STATUS_REJECTED);
        ordersMapper.updateById(order);
        return order;
    }

    @Transactional
    public Orders reject(Long orderId, Long merchantId) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatus(order, STATUS_PAID);
        restoreOrderStock(orderId);
        order.setStatus(STATUS_REJECTED);
        ordersMapper.updateById(order);
        return order;
    }

    @Transactional
    public Orders refund(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatusIn(order, STATUS_ACCEPTED, STATUS_COMPLETED);
        restoreOrderStock(orderId);
        order.setStatus(STATUS_REFUNDED);
        ordersMapper.updateById(order);
        return order;
    }

    @Transactional
    public Orders refund(Long orderId, String userId) {
        return requestRefund(orderId, userId, "用户申请退款");
    }

    @Transactional
    public Orders requestRefund(Long orderId, String userId, String reason) {
        Orders order = requireUserOrder(orderId, userId);
        requireStatusIn(order, STATUS_PAID, STATUS_ACCEPTED, STATUS_COMPLETED);
        order.setStatus(STATUS_REFUND_PENDING);
        order.setRefundStatus("PENDING");
        order.setRefundReason(reason);
        ordersMapper.updateById(order);
        notificationService.send(userId, "退款申请已提交", "订单 " + orderId + " 的退款申请已提交，等待商家处理。");
        return order;
    }

    @Transactional
    public Orders resolveRefund(Long orderId, Long merchantId, boolean approved) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatus(order, STATUS_REFUND_PENDING);
        order.setRefundStatus(approved ? "APPROVED" : "REJECTED");
        if (approved) {
            restoreOrderStock(orderId);
            order.setStatus(STATUS_REFUNDED);
            notificationService.send(order.getUserId(), "退款已通过", "订单 " + orderId + " 已退款。");
        } else {
            order.setStatus(STATUS_ACCEPTED);
            notificationService.send(order.getUserId(), "退款被拒绝", "订单 " + orderId + " 的退款申请未通过。");
        }
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

    public Orders requireUserOrder(Long orderId, String userId) {
        Orders order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("只能操作自己的订单");
        }
        return order;
    }

    public Orders requireMerchantOrder(Long orderId, Long merchantId) {
        Orders order = requireOrder(orderId);
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException("只能操作自己店铺的订单");
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

    private void requireStatusIn(Orders order, String... statuses) {
        for (String status : statuses) {
            if (status.equals(order.getStatus())) {
                return;
            }
        }
        throw new BusinessException("订单状态错误，当前状态：" + order.getStatus());
    }

    private void restoreOrderStock(Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : orderItems) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
    }
}
