package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.GeoUtils;
import com.clas.dto.BatchOrderGroupRequest;
import com.clas.dto.CreateOrderBatchRequest;
import com.clas.dto.CreateOrderBatchResponse;
import com.clas.dto.CreateOrderRequest;
import com.clas.dto.OrderPreviewResponse;
import com.clas.dto.OrderProductSummary;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final RiderSettlementService riderSettlementService;
    private final OrderLifecycleService lifecycleService;

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
        MerchantService merchantService,
        RiderSettlementService riderSettlementService,
        OrderLifecycleService lifecycleService
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
        this.riderSettlementService = riderSettlementService;
        this.lifecycleService = lifecycleService;
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
            .collect(Collectors.toMap(Product::getId, product -> product));
        List<Cart> merchantCartItems = selectCartItems(request, cartItems, products);
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
        order.setDeliveryContactName(deliverySnapshot.contactName());
        order.setDeliveryContactPhone(deliverySnapshot.contactPhone());
        order.setDeliveryLongitude(deliverySnapshot.longitude());
        order.setDeliveryLatitude(deliverySnapshot.latitude());
        order.setDistanceMeters(deliverySnapshot.distanceMeters());
        order.setRouteDistanceMeters(deliverySnapshot.routeDistanceMeters());
        order.setDeliveryStatus("WAITING");
        order.setEstimatedMinutes(deliverySnapshot.estimatedMinutes());
        order.setRefundStatus("NONE");
        order.setCreateTime(LocalDateTime.now());
        ordersMapper.insert(order);
        lifecycleService.record(order, "ORDER_CREATED", null, null, "USER", request.userId(), "用户提交订单，等待支付");
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

    @Transactional
    public CreateOrderBatchResponse createBatch(String userId, CreateOrderBatchRequest request) {
        if (request.merchantGroups() == null || request.merchantGroups().isEmpty()) {
            throw new BusinessException("请至少选择一个店铺的商品");
        }
        Set<Long> merchantIds = new HashSet<>();
        Set<Long> productIds = new HashSet<>();
        Set<Long> couponIds = new HashSet<>();
        for (BatchOrderGroupRequest group : request.merchantGroups()) {
            if (!merchantIds.add(group.merchantId())) {
                throw new BusinessException("同一店铺不能重复提交");
            }
            if (group.productIds() == null || group.productIds().isEmpty()) {
                throw new BusinessException("每个店铺至少选择一件商品");
            }
            for (Long productId : group.productIds()) {
                if (!productIds.add(productId)) {
                    throw new BusinessException("同一商品不能重复提交");
                }
            }
            if (group.userCouponId() != null && !couponIds.add(group.userCouponId())) {
                throw new BusinessException("同一优惠券不能用于多个店铺");
            }
        }

        List<OrderResponse> orders = request.merchantGroups().stream()
            .map(group -> create(new CreateOrderRequest(
                userId,
                group.merchantId(),
                request.addressId(),
                request.deliveryAddress(),
                request.remark(),
                group.userCouponId(),
                group.productIds(),
                request.deliveryContactName(),
                request.deliveryContactPhone(),
                request.deliveryLongitude(),
                request.deliveryLatitude()
            )))
            .toList();
        int totalAmount = orders.stream().mapToInt(entry -> entry.order().getTotalPrice()).sum();
        return new CreateOrderBatchResponse(orders, totalAmount);
    }

    public OrderPreviewResponse previewCheckout(
        String userId,
        Long merchantId,
        Long addressId,
        String deliveryAddress,
        BigDecimal deliveryLongitude,
        BigDecimal deliveryLatitude,
        Long userCouponId,
        List<Long> selectedProductIds
    ) {
        Merchant merchant = requireMerchant(merchantId);
        List<Cart> cartItems = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, userId));
        List<Long> productIds = cartItems.stream().map(Cart::getProductId).toList();
        Map<Long, Product> products = productIds.isEmpty()
            ? Map.of()
            : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        List<Cart> merchantCartItems = selectCartItems(
            new CreateOrderRequest(
                userId, merchantId, addressId, deliveryAddress, null, userCouponId,
                selectedProductIds, null, null, deliveryLongitude, deliveryLatitude
            ),
            cartItems,
            products
        );

        int subtotal = 0;
        String selectionError = null;
        for (Cart item : merchantCartItems) {
            Product product = products.get(item.getProductId());
            if (!"ON_SALE".equals(product.getStatus())) {
                selectionError = "商品已下架：" + product.getName();
                continue;
            }
            if (product.getStock() < item.getQuantity()) {
                selectionError = "库存不足：" + product.getName();
                continue;
            }
            subtotal += product.getPrice() * item.getQuantity();
        }

        Integer distanceMeters = null;
        int deliveryFee = calculateDeliveryFee(merchant, null);
        int minOrderPrice = merchant.getMinOrderPrice() == null ? 0 : merchant.getMinOrderPrice();
        int minOrderGap = Math.max(minOrderPrice - subtotal, 0);
        boolean canCheckout = selectionError == null && subtotal > 0 && minOrderGap == 0;
        String message = selectionError != null
            ? selectionError
            : (subtotal <= 0
            ? "当前商家购物车为空"
            : (minOrderGap > 0 ? "未达到起送价，还差 ¥" + String.format("%.2f", minOrderGap / 100.0) : "可以提交订单"));

        if ((addressId != null || deliveryAddress != null) && canCheckout) {
            try {
                DeliverySnapshot snapshot = resolveDeliverySnapshot(new CreateOrderRequest(
                    userId, merchantId, addressId, deliveryAddress, null, userCouponId,
                    null, "预览联系人", "预览电话", deliveryLongitude, deliveryLatitude
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

    public OrderPreviewResponse previewCheckout(String userId, Long merchantId, Long addressId, Long userCouponId) {
        return previewCheckout(userId, merchantId, addressId, null, null, null, userCouponId, null);
    }

    public OrderPreviewResponse previewCheckout(String userId, Long merchantId, Long addressId, Long userCouponId, List<Long> selectedProductIds) {
        return previewCheckout(userId, merchantId, addressId, null, null, null, userCouponId, selectedProductIds);
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
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        order.setStatus(STATUS_ACCEPTED);
        order.setDeliveryStatus("AVAILABLE");
        order.setAcceptedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        lifecycleService.record(order, "MERCHANT_ACCEPTED", fromStatus, fromDelivery, "SYSTEM", null, "商家已接单，订单已发布至骑手任务池");
        return order;
    }

    public Orders accept(Long orderId, Long merchantId) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatus(order, STATUS_PAID);
        if ("AVAILABLE".equals(order.getDeliveryStatus()) || "ASSIGNED_WAITING_MEAL".equals(order.getDeliveryStatus())
            || "DELIVERING".equals(order.getDeliveryStatus()) || "DELIVERED".equals(order.getDeliveryStatus())) {
            throw new BusinessException("订单已发布或已进入配送流程");
        }
        Merchant merchant = requireMerchant(merchantId);
        LocalDateTime now = LocalDateTime.now();
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        int prepareMinutes = merchant.getDefaultPrepareMinutes() == null ? 15 : merchant.getDefaultPrepareMinutes();
        int routeMinutes = order.getEstimatedMinutes() == null ? 20 : order.getEstimatedMinutes();
        LocalDateTime estimatedArrival = now.plusMinutes(prepareMinutes + routeMinutes);
        order.setStatus(STATUS_ACCEPTED);
        order.setDeliveryStatus("AVAILABLE");
        order.setPrepareMinutesSnapshot(prepareMinutes);
        order.setPromiseStartAt(estimatedArrival.minusMinutes(10));
        order.setPromiseEndAt(estimatedArrival.plusMinutes(10));
        order.setPredictedArrivalAt(estimatedArrival);
        order.setAcceptedAt(now);
        ordersMapper.updateById(order);
        lifecycleService.record(order, "MERCHANT_ACCEPTED", fromStatus, fromDelivery, "MERCHANT", String.valueOf(merchantId), "商家已接单，订单已发布至骑手任务池");
        notificationService.send(new NotificationService.NotificationTarget(
            order.getUserId(),
            "商家已接单",
            "订单 " + order.getId() + " 已确认，正在等待骑手接单。",
            "DELIVERY_STATUS",
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

    /**
     * Default fulfilment path for delivery orders: a successful payment immediately
     * enters the rider task pool. Merchant workstations only observe the order and
     * handle exceptions, rather than confirming routine steps one by one.
     */
    @Transactional
    public Orders autoAcceptAndDispatch(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, STATUS_PAID);
        Merchant merchant = requireMerchant(order.getMerchantId());
        LocalDateTime now = LocalDateTime.now();
        int prepareMinutes = merchant.getDefaultPrepareMinutes() == null ? 15 : merchant.getDefaultPrepareMinutes();
        int routeMinutes = order.getEstimatedMinutes() == null ? 20 : order.getEstimatedMinutes();
        LocalDateTime estimatedArrival = now.plusMinutes(prepareMinutes + routeMinutes);
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        order.setStatus(STATUS_ACCEPTED);
        order.setDeliveryStatus("AVAILABLE");
        order.setPrepareMinutesSnapshot(prepareMinutes);
        order.setPromiseStartAt(estimatedArrival.minusMinutes(10));
        order.setPromiseEndAt(estimatedArrival.plusMinutes(10));
        order.setPredictedArrivalAt(estimatedArrival);
        order.setAcceptedAt(now);
        ordersMapper.updateById(order);
        lifecycleService.record(order, "ORDER_AUTO_DISPATCHED", fromStatus, fromDelivery, "SYSTEM", null, "订单已自动接单并发布至骑手任务池");
        notificationService.send(new NotificationService.NotificationTarget(
            order.getUserId(), "订单已进入配送", "订单 " + order.getId() + " 已自动接单，正在等待骑手接单。",
            "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/order/" + order.getId()
        ));
        return order;
    }

    @Transactional
    public Orders readyForDispatch(Long orderId, Long merchantId) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatus(order, STATUS_ACCEPTED);
        if (!"PREPARING".equals(order.getDeliveryStatus())) {
            throw new BusinessException("订单当前不在制作中，不能发布配送", DomainErrorCode.DELIVERY_STATE_INVALID);
        }
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        order.setDeliveryStatus("AVAILABLE");
        ordersMapper.updateById(order);
        lifecycleService.record(order, "MERCHANT_READY_FOR_DISPATCH", fromStatus, fromDelivery, "MERCHANT", String.valueOf(merchantId), "餐品已制作完成，已发布至骑手任务池");
        notificationService.send(new NotificationService.NotificationTarget(
            order.getUserId(), "餐品已制作完成", "订单 " + order.getId() + " 已发布配送，正在等待骑手接单。",
            "DELIVERY_STATUS", "ORDER", order.getId(), null, null, order.getId(), merchantId, "/order/" + order.getId()
        ));
        return order;
    }

    public Orders deliver(Long orderId, Long merchantId) {
        requireMerchantOrder(orderId, merchantId);
        throw new BusinessException("商家不能完成物理配送，请由已指派骑手取餐并送达", DomainErrorCode.DELIVERY_FORBIDDEN);
    }

    public Orders complete(Long orderId) {
        Orders order = requireOrder(orderId);
        requireStatus(order, STATUS_ACCEPTED);
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        order.setStatus(STATUS_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        lifecycleService.record(order, "ORDER_COMPLETED", fromStatus, fromDelivery, "SYSTEM", null, "订单完成");
        riderSettlementService.makeCommissionWithdrawable(order);
        merchantService.refreshAveragePrice(order.getMerchantId());
        return order;
    }

    public Orders complete(Long orderId, String userId) {
        Orders order = requireUserOrder(orderId, userId);
        requireStatus(order, STATUS_ACCEPTED);
        if (!"DELIVERED".equals(order.getDeliveryStatus())) {
            throw new BusinessException("订单尚未送达，暂不能确认收货", DomainErrorCode.DELIVERY_STATE_INVALID);
        }
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        order.setStatus(STATUS_COMPLETED);
        order.setDeliveryStatus("DELIVERED");
        order.setCompletedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        lifecycleService.record(order, "USER_CONFIRMED_RECEIPT", fromStatus, fromDelivery, "USER", userId, "用户确认收货");
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
        return cancelBeforePickup(order);
    }

    @Transactional
    public Orders cancel(Long orderId, String userId) {
        Orders order = requireUserOrder(orderId, userId);
        return cancelBeforePickup(order);
    }

    private Orders cancelBeforePickup(Orders order) {
        requireStatusIn(order, STATUS_PENDING_PAYMENT, STATUS_PAID, STATUS_ACCEPTED);
        if (STATUS_ACCEPTED.equals(order.getStatus()) && ("DELIVERING".equals(order.getDeliveryStatus()) || "DELIVERED".equals(order.getDeliveryStatus()))) {
            throw new BusinessException("骑手已取餐，订单不能取消，请通过售后申请退款", DomainErrorCode.DELIVERY_STATE_INVALID);
        }
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        if (!STATUS_PENDING_PAYMENT.equals(order.getStatus())) restoreOrderStock(order.getId());
        order.setStatus(STATUS_CANCELED);
        order.setDeliveryStatus("CANCELED");
        order.setRiderId(null);
        order.setRiderAssignedAt(null);
        order.setCanceledAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        ordersMapper.clearRiderAssignment(order.getId());
        lifecycleService.record(order, "ORDER_CANCELED", fromStatus, fromDelivery, "USER", order.getUserId(), "用户取消订单");
        couponService.releaseForOrder(order.getUserCouponId());
        return order;
    }

    @Transactional
    public Orders reject(Long orderId, Long merchantId, String reason) {
        Orders order = requireMerchantOrder(orderId, merchantId);
        requireStatusIn(order, STATUS_PAID, STATUS_ACCEPTED);
        if (STATUS_ACCEPTED.equals(order.getStatus())
            && !("PREPARING".equals(order.getDeliveryStatus()) || ("AVAILABLE".equals(order.getDeliveryStatus()) && order.getRiderId() == null))) {
            throw new BusinessException("订单已发布配送或已进入骑手履约，不能拒单", DomainErrorCode.DELIVERY_STATE_INVALID);
        }
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        restoreOrderStock(orderId);
        order.setStatus(STATUS_REJECTED);
        order.setDeliveryStatus("REJECTED");
        order.setRejectReason(trimToNull(reason));
        order.setRejectedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        lifecycleService.record(order, "MERCHANT_REJECTED", fromStatus, fromDelivery, "MERCHANT", String.valueOf(merchantId), order.getRejectReason());
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
        assertRefundWithinDeliveryWindow(order);
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        order.setStatus(STATUS_REFUND_PENDING);
        order.setRefundStatus("PENDING");
        order.setRefundReason(reason);
        order.setRefundRequestedAt(LocalDateTime.now());
        order.setRefundResolvedAt(null);
        order.setRefundRejectReason(null);
        ordersMapper.updateById(order);
        lifecycleService.record(order, "REFUND_REQUESTED", fromStatus, fromDelivery, "USER", userId, "用户申请退款：" + reason);
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
            finalizeRefundApproval(order, "MERCHANT", String.valueOf(merchantId), "商家通过退款");
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
            notifyRiderRefund(order, "订单退款已通过", "订单 " + orderId + " 已退款，相关配送佣金已撤销或扣回。");
        } else {
            order.setStatus(resolveStatusAfterRefundReject(order));
            order.setRefundRejectReason(trimToNull(rejectReason));
            String reasonText = trimToNull(rejectReason);
            String content = reasonText == null
                ? "订单 " + orderId + " 的退款申请未通过。"
                : "订单 " + orderId + " 的退款申请未通过：" + reasonText;
            notificationService.send(new NotificationService.NotificationTarget(
                order.getUserId(),
                "退款申请已转平台审核",
                content + " 已自动转入平台订单争议审核。",
                "ORDER_STATUS",
                "ORDER",
                orderId,
                null,
                null,
                orderId,
                merchantId,
                "/order/" + orderId
            ));
            lifecycleService.record(order, "REFUND_REJECTED", "REFUND_PENDING", order.getDeliveryStatus(), "MERCHANT", String.valueOf(merchantId), "商家拒绝退款：" + (reasonText == null ? "未填写理由" : reasonText));
        }
        ordersMapper.updateById(order);
        return order;
    }

    /** Final administrator decision for an already submitted refund dispute. */
    @Transactional
    public Orders approveRefundByAdmin(Orders order, String adminId, String reason) {
        requireStatus(order, STATUS_REFUND_PENDING);
        if (!"DISPUTE_PENDING".equals(order.getRefundStatus())) {
            throw new BusinessException("订单当前不处于待裁定退款争议状态");
        }
        order.setRefundStatus("APPROVED");
        order.setRefundResolvedAt(LocalDateTime.now());
        finalizeRefundApproval(order, "ADMIN", adminId, "管理员通过退款争议：" + reason);
        ordersMapper.updateById(order);
        notificationService.send(new NotificationService.NotificationTarget(
            order.getUserId(), "退款争议已通过", "订单 " + order.getId() + " 的退款争议已通过，退款已处理。",
            "ORDER_STATUS", "ORDER", order.getId(), null, null, order.getId(), order.getMerchantId(), "/order/" + order.getId()
        ));
        notifyRiderRefund(order, "订单退款争议已通过", "订单 " + order.getId() + " 已退款，相关配送佣金已撤销或扣回。");
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
        return enrichResponses(orders, itemsByOrderId, false);
    }

    private OrderResponse withItems(Orders order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return enrichResponses(List.of(order), Map.of(order.getId(), orderItems), false).get(0);
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
        return enrichResponses(orders, itemsByOrderId, true);
    }

    private OrderResponse withItemsForMerchant(Orders order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return enrichResponses(List.of(order), Map.of(order.getId(), orderItems), true).get(0);
    }

    private List<OrderResponse> enrichResponses(
        List<Orders> orders,
        Map<Long, List<OrderItem>> itemsByOrderId,
        boolean includeCustomerCallUrl
    ) {
        Set<Long> productIds = itemsByOrderId.values().stream()
            .flatMap(List::stream)
            .map(OrderItem::getProductId)
            .collect(Collectors.toSet());
        Map<Long, Product> productsById = productIds.isEmpty()
            ? Map.of()
            : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        Set<Long> merchantIds = orders.stream().map(Orders::getMerchantId).collect(Collectors.toSet());
        Map<Long, Merchant> merchantsById = merchantIds.isEmpty()
            ? Map.of()
            : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, merchant -> merchant));

        return orders.stream().map(order -> {
            List<OrderItem> items = itemsByOrderId.getOrDefault(order.getId(), List.of());
            List<OrderProductSummary> products = items.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .map(productsById::get)
                .filter(java.util.Objects::nonNull)
                .map(product -> new OrderProductSummary(product.getId(), product.getName(), product.getImage()))
                .toList();
            Merchant merchant = merchantsById.get(order.getMerchantId());
            return new OrderResponse(
                order,
                items,
                includeCustomerCallUrl ? customerCallUrl(order) : null,
                merchant == null ? null : merchant.getMerchantName(),
                merchant == null ? null : merchant.getLogo(),
                products
            );
        }).toList();
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
        if ("DELIVERING".equals(order.getDeliveryStatus()) || "ASSIGNED_WAITING_MEAL".equals(order.getDeliveryStatus())
            || "AVAILABLE".equals(order.getDeliveryStatus()) || "PREPARING".equals(order.getDeliveryStatus())) {
            return STATUS_ACCEPTED;
        }
        return STATUS_PAID;
    }

    private void assertRefundWithinDeliveryWindow(Orders order) {
        if (!"DELIVERED".equals(order.getDeliveryStatus())) return;
        LocalDateTime deliveredAt = order.getDeliveryCompletedAt() == null ? order.getDeliveredAt() : order.getDeliveryCompletedAt();
        if (deliveredAt == null || LocalDateTime.now().isAfter(deliveredAt.plusMinutes(15))) {
            throw new BusinessException("订单送达超过 15 分钟，已无法发起退款申请");
        }
    }

    private void finalizeRefundApproval(Orders order, String actorRole, String actorId, String remark) {
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        if (order.getPickedUpAt() == null && !"DELIVERING".equals(order.getDeliveryStatus()) && !"DELIVERED".equals(order.getDeliveryStatus())) {
            restoreOrderStock(order.getId());
        }
        order.setStatus(STATUS_REFUNDED);
        riderSettlementService.reverseCommissionForRefund(order);
        lifecycleService.record(order, "REFUND_APPROVED", fromStatus, fromDelivery, actorRole, actorId, remark);
    }

    private void notifyRiderRefund(Orders order, String title, String content) {
        if (order.getRiderId() == null) return;
        notificationService.send(new NotificationService.NotificationTarget(
            order.getRiderId(), title, content, "DELIVERY_STATUS", "ORDER", order.getId(), null, null,
            order.getId(), order.getMerchantId(), "/rider/profile"
        ));
    }

    private List<Cart> selectCartItems(
        CreateOrderRequest request,
        List<Cart> cartItems,
        Map<Long, Product> products
    ) {
        Map<Long, Cart> cartByProductId = cartItems.stream()
            .collect(Collectors.toMap(Cart::getProductId, item -> item));
        List<Long> requestedProductIds = request.productIds();
        if (requestedProductIds == null) {
            List<Cart> merchantItems = cartItems.stream()
                .filter(item -> {
                    Product product = products.get(item.getProductId());
                    return product != null && request.merchantId().equals(product.getMerchantId());
                })
                .toList();
            if (merchantItems.isEmpty()) {
                throw new BusinessException("该商家的购物车为空");
            }
            return merchantItems;
        }

        if (requestedProductIds.isEmpty()) {
            throw new BusinessException("请至少选择一件商品");
        }
        LinkedHashSet<Long> uniqueProductIds = new LinkedHashSet<>(requestedProductIds);
        if (uniqueProductIds.size() != requestedProductIds.size()) {
            throw new BusinessException("选择的商品不能重复");
        }
        return uniqueProductIds.stream().map(productId -> {
            Cart cart = cartByProductId.get(productId);
            Product product = products.get(productId);
            if (cart == null || product == null) {
                throw new BusinessException("所选商品不在当前购物车中");
            }
            if (!request.merchantId().equals(product.getMerchantId())) {
                throw new BusinessException("所选商品不属于当前店铺");
            }
            return cart;
        }).toList();
    }

    private DeliverySnapshot resolveDeliverySnapshot(CreateOrderRequest request) {
        String addressText;
        String contactName;
        String contactPhone;
        BigDecimal longitude;
        BigDecimal latitude;
        if (request.addressId() == null) {
            addressText = requireDeliveryValue(request.deliveryAddress(), "请填写配送地址", 255);
            contactName = requireDeliveryValue(request.deliveryContactName(), "请填写联系人", 50);
            contactPhone = requireDeliveryValue(request.deliveryContactPhone(), "请填写联系电话", 20);
            longitude = request.deliveryLongitude();
            latitude = request.deliveryLatitude();
            if (!GeoUtils.hasCoordinate(longitude, latitude)) {
                throw new BusinessException("请通过自动定位或手动选择确认配送位置");
            }
        } else {
            UserAddress address = userAddressMapper.selectById(request.addressId());
            if (address == null || !request.userId().equals(address.getUserId())) {
                throw new BusinessException("地址不存在或无权操作");
            }
            if (!GeoUtils.hasCoordinate(address.getLongitude(), address.getLatitude())) {
                throw new BusinessException("该地址缺少地图坐标");
            }
            addressText = address.getAddress();
            contactName = firstNonBlank(request.deliveryContactName(), address.getContactName());
            contactPhone = firstNonBlank(request.deliveryContactPhone(), address.getPhone());
            longitude = address.getLongitude();
            latitude = address.getLatitude();
        }
        Merchant merchant = merchantMapper.selectById(request.merchantId());
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        if (!GeoUtils.hasCoordinate(merchant.getLongitude(), merchant.getLatitude())) {
            throw new BusinessException("商家缺少地图坐标，暂不支持配送");
        }

        int distanceMeters = GeoUtils.distanceMeters(
            latitude,
            longitude,
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
            longitude,
            latitude
        );
        Integer routeDistanceMeters = route.map(AmapRouteService.RouteEstimate::distanceMeters).orElse(null);
        int estimatedMinutes = route
            .map(AmapRouteService.RouteEstimate::durationMinutes)
            .map(minutes -> Math.max(20, minutes + 10))
            .orElseGet(() -> estimateMinutes(distanceMeters));

        return new DeliverySnapshot(
            addressText,
            contactName,
            contactPhone,
            longitude,
            latitude,
            distanceMeters,
            routeDistanceMeters,
            estimatedMinutes
        );
    }

    public void assertReadyForPayment(Orders order) {
        requireDeliveryValue(order.getDeliveryAddress(), "请先完善配送地址", 255);
        requireDeliveryValue(order.getDeliveryContactName(), "请先完善联系人", 50);
        requireDeliveryValue(order.getDeliveryContactPhone(), "请先完善联系电话", 20);
        if (!GeoUtils.hasCoordinate(order.getDeliveryLongitude(), order.getDeliveryLatitude())) {
            throw new BusinessException("请先通过自动定位或手动选择确认配送位置");
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        String normalized = trimToNull(preferred);
        return normalized == null ? trimToNull(fallback) : normalized;
    }

    private String requireDeliveryValue(String value, String message, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(message + "，且不能超过 " + maxLength + " 个字符");
        }
        return normalized;
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
        String contactName,
        String contactPhone,
        BigDecimal longitude,
        BigDecimal latitude,
        Integer distanceMeters,
        Integer routeDistanceMeters,
        Integer estimatedMinutes
    ) {
    }
}
