package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.GeoUtils;
import com.clas.dto.CreateOrderRequest;
import com.clas.dto.OrderPreviewResponse;
import com.clas.dto.OrderResponse;
import com.clas.entity.Cart;
import com.clas.entity.Merchant;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.entity.Product;
import com.clas.entity.UserAddress;
import com.clas.mapper.CartMapper;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.OrderItemMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ProductMapper;
import com.clas.mapper.UserAddressMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final MerchantMapper merchantMapper;
    private final UserAddressMapper userAddressMapper;
    private final AmapRouteService amapRouteService;
    private final PenaltyService penaltyService;
    private final CouponService couponService;
    private final MerchantService merchantService;

    public OrderService(
        OrdersMapper ordersMapper,
        OrderItemMapper orderItemMapper,
        CartMapper cartMapper,
        ProductMapper productMapper,
        NotificationService notificationService,
        MerchantMapper merchantMapper,
        UserAddressMapper userAddressMapper,
        AmapRouteService amapRouteService,
        PenaltyService penaltyService,
        CouponService couponService,
        MerchantService merchantService
    ) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.notificationService = notificationService;
        this.merchantMapper = merchantMapper;
        this.userAddressMapper = userAddressMapper;
        this.amapRouteService = amapRouteService;
        this.penaltyService = penaltyService;
        this.couponService = couponService;
        this.merchantService = merchantService;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        penaltyService.assertCanUsePlatform(request.userId());
        Merchant merchant = requireMerchant(request.merchantId());
        assertMerchantOpenNow(merchant);
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
        DeliverySnapshot deliverySnapshot = resolveDeliverySnapshot(request);

        int subtotal = 0;
        for (Cart item : merchantCartItems) {
            Product product = products.get(item.getProductId());
            if (!"ON_SALE".equals(product.getStatus())) {
                throw new BusinessException("商品已下架：" + product.getName());
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException("库存不足：" + product.getName());
            }
            subtotal += product.getPrice() * item.getQuantity();
        }

        int deliveryFee = calculateDeliveryFee(merchant, deliverySnapshot.distanceMeters());
        int minOrderPrice = merchant.getMinOrderPrice() == null ? 0 : merchant.getMinOrderPrice();
        if (subtotal < minOrderPrice) {
            throw new BusinessException("未达到起送价，还差 ¥" + String.format("%.2f", (minOrderPrice - subtotal) / 100.0));
        }
        int couponDiscount = couponService.calculateDiscount(
            request.userCouponId(),
            request.userId(),
            request.merchantId(),
            subtotal
        );
        int totalPrice = Math.max(subtotal + deliveryFee - couponDiscount, 0);

        Orders order = new Orders();
        order.setUserId(request.userId());
        order.setMerchantId(request.merchantId());
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setCouponDiscount(couponDiscount);
        order.setUserCouponId(request.userCouponId());
        order.setTotalPrice(totalPrice);
        order.setRemark(trimToNull(request.remark()));
        order.setStatus(STATUS_PENDING_PAYMENT);
        order.setDeliveryAddress(deliverySnapshot.address());
        order.setDeliveryLongitude(deliverySnapshot.longitude());
        order.setDeliveryLatitude(deliverySnapshot.latitude());
        order.setDistanceMeters(deliverySnapshot.distanceMeters());
        order.setRouteDistanceMeters(deliverySnapshot.routeDistanceMeters());
        order.setDeliveryStatus("WAITING");
        order.setEstimatedMinutes(deliverySnapshot.estimatedMinutes());
        order.setRefundStatus("NONE");
        order.setCreateTime(LocalDateTime.now());
        ordersMapper.insert(order);
        couponService.reserveForOrder(request.userCouponId(), request.userId(), order.getId());

        List<OrderItem> orderItems = merchantCartItems.stream().map(cart -> {
            Product product = products.get(cart.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItemMapper.insert(orderItem);
            cartMapper.deleteById(cart.getId());
            return orderItem;
        }).toList();

        notificationService.send(new NotificationService.NotificationTarget(
            order.getUserId(),
            "订单已创建",
            "订单 " + order.getId() + " 已创建，请及时完成支付。",
            "ORDER_STATUS",
            "ORDER",
            order.getId(),
            null,
            null,
            order.getId(),
            order.getMerchantId(),
            "/order/" + order.getId()
        ));
        return new OrderResponse(order, orderItems);
    }

    public OrderPreviewResponse previewCheckout(String userId, Long merchantId, Long addressId, Long userCouponId) {
        Merchant merchant = requireMerchant(merchantId);
        List<Cart> cartItems = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, userId));
        List<Long> productIds = cartItems.stream().map(Cart::getProductId).toList();
        Map<Long, Product> products = productIds.isEmpty()
            ? Map.of()
            : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        int subtotal = 0;
        for (Cart item : cartItems) {
            Product product = products.get(item.getProductId());
            if (product == null || !merchantId.equals(product.getMerchantId())) {
                continue;
            }
            if (!"ON_SALE".equals(product.getStatus()) || product.getStock() < item.getQuantity()) {
                continue;
            }
            subtotal += product.getPrice() * item.getQuantity();
        }

        Integer distanceMeters = null;
        int deliveryFee = calculateDeliveryFee(merchant, null);
        int minOrderPrice = merchant.getMinOrderPrice() == null ? 0 : merchant.getMinOrderPrice();
        int minOrderGap = Math.max(minOrderPrice - subtotal, 0);
        boolean canCheckout = subtotal > 0 && minOrderGap == 0;
        String message = subtotal <= 0
            ? "当前商家购物车为空"
            : (minOrderGap > 0 ? "未达到起送价，还差 ¥" + String.format("%.2f", minOrderGap / 100.0) : "可以提交订单");

        if (addressId != null && canCheckout) {
            try {
                DeliverySnapshot snapshot = resolveDeliverySnapshot(new CreateOrderRequest(
                    userId, merchantId, addressId, null, null, userCouponId
                ));
                distanceMeters = snapshot.distanceMeters();
                deliveryFee = calculateDeliveryFee(merchant, distanceMeters);
            } catch (BusinessException exception) {
                canCheckout = false;
                message = exception.getMessage();
                deliveryFee = 0;
                distanceMeters = null;
            }
        }

        int couponDiscount = 0;
        if (userCouponId != null && subtotal > 0) {
            try {
                couponDiscount = couponService.calculateDiscount(userCouponId, userId, merchantId, subtotal);
            } catch (BusinessException exception) {
                canCheckout = false;
                message = exception.getMessage();
            }
        }

        int totalPrice = Math.max(subtotal + deliveryFee - couponDiscount, 0);
        List<com.clas.dto.UserCouponResponse> availableCoupons = subtotal > 0
            ? couponService.listAvailableForCheckout(userId, merchantId, subtotal)
            : List.of();

        return new OrderPreviewResponse(
            merchantId,
            subtotal,
            deliveryFee,
            distanceMeters,
            minOrderPrice,
            minOrderGap,
            couponDiscount,
            userCouponId,
            totalPrice,
            canCheckout,
            message,
            availableCoupons
        );
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
        return withItemsForMerchant(orders);
    }

    public List<OrderResponse> listForMerchantAndUser(Long merchantId, String userId) {
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getMerchantId, merchantId)
            .eq(Orders::getUserId, userId)
            .orderByDesc(Orders::getCreateTime));
        return withItemsForMerchant(orders);
    }

    public OrderResponse getForUser(Long orderId, String userId) {
        return withItems(requireUserOrder(orderId, userId));
    }

    public OrderResponse getForMerchant(Long orderId, Long merchantId) {
        return withItemsForMerchant(requireMerchantOrder(orderId, merchantId));
    }

    public OrderResponse getForAdmin(Long orderId) {
        return withItems(requireOrder(orderId));
    }

    public Orders accept(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, STATUS_PAID);
        order.setStatus(STATUS_ACCEPTED);
        order.setAcceptedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        return order;
    }

    public Orders accept(Long orderId, Long merchantId) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatus(order, STATUS_PAID);
        order.setStatus(STATUS_ACCEPTED);
        order.setDeliveryStatus("PREPARING");
        order.setAcceptedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        notificationService.send(new NotificationService.NotificationTarget(
            order.getUserId(),
            "已支付(自动接单中)",
            "订单 " + order.getId() + " 正在备餐。",
            "ORDER_STATUS",
            "ORDER",
            order.getId(),
            null,
            null,
            order.getId(),
            merchantId,
            "/order/" + order.getId()
        ));
        return order;
    }

    public Orders deliver(Long orderId, Long merchantId) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatus(order, STATUS_ACCEPTED);
        order.setDeliveryStatus("DELIVERING");
        order.setEstimatedMinutes(15);
        order.setDeliveredAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        notificationService.send(new NotificationService.NotificationTarget(
            order.getUserId(),
            "订单配送中",
            "订单 " + order.getId() + " 已进入配送流程。",
            "ORDER_STATUS",
            "ORDER",
            order.getId(),
            null,
            null,
            order.getId(),
            merchantId,
            "/order/" + order.getId()
        ));
        return order;
    }

    public Orders complete(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, STATUS_ACCEPTED);
        order.setStatus(STATUS_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        merchantService.refreshAveragePrice(order.getMerchantId());
        return order;
    }

    public Orders complete(Long orderId, String userId) {
        Orders order = requireUserOrder(orderId, userId);
        requireStatus(order, STATUS_ACCEPTED);
        order.setStatus(STATUS_COMPLETED);
        order.setDeliveryStatus("DELIVERED");
        order.setCompletedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        merchantService.refreshAveragePrice(order.getMerchantId());
        notificationService.send(new NotificationService.NotificationTarget(
            order.getUserId(),
            "订单已完成",
            "订单 " + order.getId() + " 已完成，欢迎评价本次体验。",
            "ORDER_STATUS",
            "ORDER",
            order.getId(),
            null,
            null,
            order.getId(),
            order.getMerchantId(),
            "/order/" + order.getId()
        ));
        return order;
    }

    @Transactional
    public Orders cancel(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatusIn(order, STATUS_PENDING_PAYMENT, STATUS_PAID);
        if (STATUS_PAID.equals(order.getStatus())) {
            restoreOrderStock(orderId);
        }
        order.setStatus(STATUS_CANCELED);
        order.setCanceledAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        couponService.releaseForOrder(order.getUserCouponId());
        return order;
    }

    @Transactional
    public Orders cancel(Long orderId, String userId) {
        Orders order = requireUserOrder(orderId, userId);
        requireStatusIn(order, STATUS_PENDING_PAYMENT, STATUS_PAID);
        if (STATUS_PAID.equals(order.getStatus())) {
            restoreOrderStock(orderId);
        }
        order.setStatus(STATUS_CANCELED);
        order.setCanceledAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        couponService.releaseForOrder(order.getUserCouponId());
        return order;
    }

    @Transactional
    public Orders reject(Long orderId, Long merchantId, String reason) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatusIn(order, STATUS_PAID, STATUS_ACCEPTED);
        restoreOrderStock(orderId);
        order.setStatus(STATUS_REJECTED);
        order.setRejectReason(trimToNull(reason));
        order.setRejectedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        String rejectText = order.getRejectReason() == null ? "商家暂时无法接单" : order.getRejectReason();
        notificationService.send(new NotificationService.NotificationTarget(
            order.getUserId(),
            "商家已拒单",
            "订单 " + order.getId() + " 已被拒单：" + rejectText,
            "ORDER_STATUS",
            "ORDER",
            order.getId(),
            null,
            null,
            order.getId(),
            merchantId,
            "/order/" + order.getId()
        ));
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
        order.setRefundRequestedAt(LocalDateTime.now());
        order.setRefundResolvedAt(null);
        order.setRefundRejectReason(null);
        ordersMapper.updateById(order);
        notificationService.send(new NotificationService.NotificationTarget(
            userId,
            "退款申请已提交",
            "订单 " + orderId + " 的退款申请已提交，等待商家处理。",
            "ORDER_STATUS",
            "ORDER",
            orderId,
            null,
            null,
            orderId,
            order.getMerchantId(),
            "/order/" + orderId
        ));
        return order;
    }

    @Transactional
    public Orders resolveRefund(Long orderId, Long merchantId, boolean approved, String rejectReason) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatus(order, STATUS_REFUND_PENDING);
        order.setRefundStatus(approved ? "APPROVED" : "REJECTED");
        order.setRefundResolvedAt(LocalDateTime.now());
        if (approved) {
            restoreOrderStock(orderId);
            order.setStatus(STATUS_REFUNDED);
            notificationService.send(new NotificationService.NotificationTarget(
                order.getUserId(),
                "退款已通过",
                "订单 " + orderId + " 已退款。",
                "ORDER_STATUS",
                "ORDER",
                orderId,
                null,
                null,
                orderId,
                merchantId,
                "/order/" + orderId
            ));
        } else {
            order.setStatus(resolveStatusAfterRefundReject(order));
            order.setRefundRejectReason(trimToNull(rejectReason));
            String reasonText = trimToNull(rejectReason);
            String content = reasonText == null
                ? "订单 " + orderId + " 的退款申请未通过。"
                : "订单 " + orderId + " 的退款申请未通过：" + reasonText;
            notificationService.send(new NotificationService.NotificationTarget(
                order.getUserId(),
                "退款被拒绝",
                content,
                "ORDER_STATUS",
                "ORDER",
                orderId,
                null,
                null,
                orderId,
                merchantId,
                "/order/" + orderId
            ));
        }
        ordersMapper.updateById(order);
        return order;
    }

    public Orders resolveRefund(Long orderId, Long merchantId, boolean approved) {
        return resolveRefund(orderId, merchantId, approved, null);
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
            throw new BusinessException("只能操作自己的订单", DomainErrorCode.AUTH_FORBIDDEN);
        }
        return order;
    }

    public Orders requireMerchantOrder(Long orderId, Long merchantId) {
        Orders order = requireOrder(orderId);
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException("只能操作自己店铺的订单", DomainErrorCode.AUTH_FORBIDDEN);
        }
        return order;
    }

    private List<OrderResponse> withItems(List<Orders> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Orders::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
            .stream()
            .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream()
            .map(order -> new OrderResponse(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
            .toList();
    }

    private OrderResponse withItems(Orders order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return new OrderResponse(order, orderItems);
    }

    private List<OrderResponse> withItemsForMerchant(List<Orders> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Orders::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
            .stream()
            .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream()
            .map(order -> new OrderResponse(
                order,
                itemsByOrderId.getOrDefault(order.getId(), List.of()),
                customerCallUrl(order)
            ))
            .toList();
    }

    private OrderResponse withItemsForMerchant(Orders order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return new OrderResponse(order, orderItems, customerCallUrl(order));
    }

    private String customerCallUrl(Orders order) {
        String phone = order.getUserId();
        return phone == null || phone.isBlank() ? null : "tel:" + phone.trim();
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

    public void deductStockForPayment(Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : orderItems) {
            Product product = productMapper.selectById(item.getProductId());
            String productName = product != null ? product.getName() : "商品#" + item.getProductId();
            int rows = productMapper.deductStock(item.getProductId(), item.getQuantity());
            if (rows == 0) {
                throw new BusinessException("库存不足：" + productName);
            }
        }
    }

    private String resolveStatusAfterRefundReject(Orders order) {
        if ("DELIVERED".equals(order.getDeliveryStatus())) {
            return STATUS_COMPLETED;
        }
        if ("DELIVERING".equals(order.getDeliveryStatus()) || "PREPARING".equals(order.getDeliveryStatus())) {
            return STATUS_ACCEPTED;
        }
        return STATUS_PAID;
    }

    private DeliverySnapshot resolveDeliverySnapshot(CreateOrderRequest request) {
        if (request.addressId() == null) {
            return new DeliverySnapshot(request.deliveryAddress(), null, null, null, null, 30);
        }
        UserAddress address = userAddressMapper.selectById(request.addressId());
        if (address == null || !request.userId().equals(address.getUserId())) {
            throw new BusinessException("地址不存在或无权操作");
        }
        if (!GeoUtils.hasCoordinate(address.getLongitude(), address.getLatitude())) {
            throw new BusinessException("该地址缺少地图坐标");
        }
        Merchant merchant = merchantMapper.selectById(request.merchantId());
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        if (!GeoUtils.hasCoordinate(merchant.getLongitude(), merchant.getLatitude())) {
            throw new BusinessException("商家缺少地图坐标，暂不支持配送");
        }

        int distanceMeters = GeoUtils.distanceMeters(
            address.getLatitude(),
            address.getLongitude(),
            merchant.getLatitude(),
            merchant.getLongitude()
        );
        int radius = merchant.getDeliveryRadiusM() == null ? 3000 : merchant.getDeliveryRadiusM();
        if (distanceMeters > radius) {
            throw new BusinessException("收货地址超出商家配送范围");
        }

        Optional<AmapRouteService.RouteEstimate> route = amapRouteService.estimateDriving(
            merchant.getLongitude(),
            merchant.getLatitude(),
            address.getLongitude(),
            address.getLatitude()
        );
        Integer routeDistanceMeters = route.map(AmapRouteService.RouteEstimate::distanceMeters).orElse(null);
        int estimatedMinutes = route
            .map(AmapRouteService.RouteEstimate::durationMinutes)
            .map(minutes -> Math.max(20, minutes + 10))
            .orElseGet(() -> estimateMinutes(distanceMeters));

        return new DeliverySnapshot(
            address.getAddress(),
            address.getLongitude(),
            address.getLatitude(),
            distanceMeters,
            routeDistanceMeters,
            estimatedMinutes
        );
    }

    private int estimateMinutes(int distanceMeters) {
        return Math.max(20, 20 + (int) Math.ceil(distanceMeters / 500.0) * 5);
    }

    public static int calculateDeliveryFee(Merchant merchant, Integer distanceMeters) {
        int baseFee = merchant.getDeliveryFee() == null ? 0 : merchant.getDeliveryFee();
        if (distanceMeters == null || distanceMeters <= 2000) {
            return baseFee;
        }
        int extraKm = (int) Math.ceil((distanceMeters - 2000) / 1000.0);
        return baseFee + extraKm * 100;
    }

    public void markCouponUsed(Long orderId) {
        Orders order = requireOrder(orderId);
        couponService.markUsed(order.getUserCouponId(), orderId);
    }

    private Merchant requireMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        return merchant;
    }

    private void assertMerchantOpenNow(Merchant merchant) {
        if (Boolean.TRUE.equals(merchant.getManualClosed())) {
            throw new BusinessException("商家已打烊");
        }
        String businessHours = trimToNull(merchant.getBusinessHours());
        if (businessHours == null) {
            return;
        }
        String[] parts = businessHours.split("-");
        if (parts.length != 2) {
            return;
        }
        try {
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            LocalTime now = LocalTime.now();
            if (!isWithinBusinessHours(now, start, end)) {
                throw new BusinessException("商家已休息，当前营业时间：" + businessHours);
            }
        } catch (DateTimeParseException exception) {
            return;
        }
    }

    private boolean isWithinBusinessHours(LocalTime now, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record DeliverySnapshot(
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        Integer distanceMeters,
        Integer routeDistanceMeters,
        Integer estimatedMinutes
    ) {
    }
}
