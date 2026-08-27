package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.dto.BatchPaymentRequest;
import com.clas.dto.BatchPaymentResponse;
import com.clas.dto.PaymentRequest;
import com.clas.dto.PaymentResponse;
import com.clas.entity.Orders;
import com.clas.entity.Payment;
import com.clas.mapper.OrdersMapper;
import com.clas.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String PAY_METHOD_FAIL_MOCK = "FAIL_MOCK";
    private static final String ORDER_STATUS_PAYING = "PAYING";

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final OrdersMapper ordersMapper;
    private final TransactionTemplate transactionTemplate;
    private final OrderLifecycleService lifecycleService;

    public PaymentService(
        PaymentRepository paymentRepository,
        OrderService orderService,
        OrdersMapper ordersMapper,
        TransactionTemplate transactionTemplate,
        OrderLifecycleService lifecycleService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.ordersMapper = ordersMapper;
        this.transactionTemplate = transactionTemplate;
        this.lifecycleService = lifecycleService;
    }

    public PaymentResponse mockPay(PaymentRequest request) {
        Orders order = orderService.requireUserOrder(request.orderId(), request.userId());
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        if (idempotencyKey != null) {
            var existingPayment = paymentRepository.findByUserIdAndIdempotencyKey(request.userId(), idempotencyKey);
            if (existingPayment.isPresent()) {
                Payment payment = existingPayment.get();
                if (!order.getId().equals(payment.getOrderId())) {
                    throw new BusinessException("幂等键已用于其他订单");
                }
                return PaymentResponse.from(payment, order.getStatus());
            }
        }
        if (!OrderService.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            if (OrderService.STATUS_PAID.equals(order.getStatus())
                || OrderService.STATUS_ACCEPTED.equals(order.getStatus())
                || OrderService.STATUS_COMPLETED.equals(order.getStatus())) {
                return paymentRepository.findSuccessfulByOrderId(order.getId())
                    .map(payment -> PaymentResponse.from(payment, order.getStatus()))
                    .orElseGet(() -> PaymentResponse.from(buildSyntheticSuccessPayment(order), order.getStatus()));
            }
            throw new BusinessException("订单当前不可支付，状态：" + order.getStatus());
        }

        return paymentRepository.findSuccessfulByOrderId(order.getId())
            .map(payment -> {
                if (OrderService.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
                    Orders paidOrder = markOrderPaidAwaitingMerchant(order);
                    return PaymentResponse.from(payment, paidOrder.getStatus());
                }
                return PaymentResponse.from(payment, order.getStatus());
            })
            .or(() -> paymentRepository.findLatestByOrderId(order.getId())
                .filter(payment -> STATUS_FAILED.equals(payment.getStatus())
                    && normalizePayMethod(request.payMethod()).equals(payment.getPayMethod()))
                .map(payment -> PaymentResponse.from(payment, order.getStatus())))
            .orElseGet(() -> beginAndConfirmPayment(order, request));
    }

    public PaymentResponse getPaymentStatus(Long orderId) {
        Orders order = orderService.requireOrder(orderId);
        return paymentStatusForOrder(orderId, order);
    }

    public PaymentResponse getPaymentStatus(Long orderId, String userId) {
        Orders order = orderService.requireUserOrder(orderId, userId);
        return paymentStatusForOrder(orderId, order);
    }

    public BatchPaymentResponse getBatchPaymentStatus(List<Long> orderIds, String userId) {
        List<Long> normalizedIds = normalizeOrderIds(orderIds);
        List<PaymentResponse> payments = normalizedIds.stream()
            .map(orderId -> getPaymentStatus(orderId, userId))
            .toList();
        return toBatchResponse(payments);
    }

    public BatchPaymentResponse mockPayBatch(BatchPaymentRequest request, String userId) {
        List<Long> normalizedIds = normalizeOrderIds(request.orderIds());
        normalizedIds.forEach(orderId -> orderService.requireUserOrder(orderId, userId));

        String batchKey = normalizeIdempotencyKey(request.idempotencyKey());
        List<PaymentResponse> payments = normalizedIds.stream().map(orderId -> {
            try {
                String orderKey = batchKey == null ? null : batchKey.substring(0, Math.min(batchKey.length(), 96)) + ":" + orderId;
                return mockPay(new PaymentRequest(orderId, userId, request.payMethod(), orderKey));
            } catch (BusinessException exception) {
                return getPaymentStatus(orderId, userId);
            }
        }).toList();
        return toBatchResponse(payments);
    }

    private PaymentResponse beginAndConfirmPayment(Orders order, PaymentRequest request) {
        String payMethod = normalizePayMethod(request.payMethod());
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(request.userId());
        payment.setAmount(order.getTotalPrice());
        payment.setPayMethod(payMethod);
        payment.setStatus(STATUS_PENDING);
        payment.setCreateTime(LocalDateTime.now());
        payment.setIdempotencyKey(idempotencyKey);
        paymentRepository.save(payment);

        try {
            Thread.sleep(100); // 模拟支付延迟（生产环境应异步处理）
            if (PAY_METHOD_FAIL_MOCK.equals(payMethod)) {
                return transactionTemplate.execute(status -> failPayment(payment, order));
            }
            PaymentOutcome outcome = transactionTemplate.execute(status -> confirmPayment(payment, order));
            if (outcome.error() != null) {
                throw outcome.error();
            }
            return outcome.response();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            transactionTemplate.executeWithoutResult(status -> failPayment(payment, order));
            throw new BusinessException("模拟支付被中断");
        }
    }

    private PaymentOutcome confirmPayment(Payment payment, Orders order) {
        int locked = ordersMapper.updateStatusIfCurrent(
            order.getId(),
            OrderService.STATUS_PENDING_PAYMENT,
            ORDER_STATUS_PAYING
        );
        if (locked == 0) {
            Orders latest = orderService.requireOrder(order.getId());
            PaymentResponse response = paymentRepository.findSuccessfulByOrderId(order.getId())
                .map(existing -> PaymentResponse.from(existing, latest.getStatus()))
                .orElseGet(() -> PaymentResponse.from(payment, latest.getStatus()));
            return new PaymentOutcome(response, null);
        }

        try {
            orderService.deductStockForPayment(order.getId());
            orderService.markCouponUsed(order.getId());
            payment.setStatus(STATUS_SUCCESS);
            paymentRepository.save(payment);
            ordersMapper.updateStatusIfCurrent(order.getId(), ORDER_STATUS_PAYING, OrderService.STATUS_PAID);
            Orders paidOrder = markOrderPaidAwaitingMerchant(order);
            return new PaymentOutcome(PaymentResponse.from(payment, paidOrder.getStatus()), null);
        } catch (RuntimeException exception) {
            payment.setStatus(STATUS_FAILED);
            paymentRepository.save(payment);
            ordersMapper.updateStatusIfCurrent(order.getId(), ORDER_STATUS_PAYING, OrderService.STATUS_PENDING_PAYMENT);
            BusinessException error = exception instanceof BusinessException businessException
                ? businessException
                : new BusinessException("支付失败，请稍后重试");
            return new PaymentOutcome(PaymentResponse.from(payment, OrderService.STATUS_PENDING_PAYMENT), error);
        }
    }

    private PaymentResponse failPayment(Payment payment, Orders order) {
        payment.setStatus(STATUS_FAILED);
        paymentRepository.save(payment);
        return PaymentResponse.from(payment, order.getStatus());
    }

    private Orders markOrderPaidAwaitingMerchant(Orders order) {
        LocalDateTime now = LocalDateTime.now();
        String fromStatus = order.getStatus();
        String fromDelivery = order.getDeliveryStatus();
        order.setStatus(OrderService.STATUS_PAID);
        order.setDeliveryStatus("WAITING");
        order.setPaidAt(now);
        ordersMapper.updateById(order);
        lifecycleService.record(order, "PAYMENT_SUCCEEDED", fromStatus, fromDelivery, "USER", order.getUserId(), "支付成功，等待商家接单");
        return order;
    }

    private PaymentResponse paymentStatusForOrder(Long orderId, Orders order) {
        return paymentRepository.findLatestByOrderId(orderId)
            .map(payment -> PaymentResponse.from(payment, order.getStatus()))
            .orElseGet(() -> new PaymentResponse(
                null,
                orderId,
                order.getTotalPrice(),
                null,
                mapOrderStatusToPaymentStatus(order.getStatus()),
                order.getStatus(),
                null,
                null
            ));
    }

    private Payment buildSyntheticSuccessPayment(Orders order) {
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(order.getUserId());
        payment.setAmount(order.getTotalPrice());
        payment.setPayMethod("MOCK");
        payment.setStatus(STATUS_SUCCESS);
        payment.setCreateTime(order.getCreateTime());
        return payment;
    }

    private String normalizePayMethod(String payMethod) {
        return payMethod == null || payMethod.isBlank() ? "MOCK" : payMethod.trim();
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 128) {
            throw new BusinessException("幂等键长度不能超过128个字符");
        }
        return normalized;
    }

    private List<Long> normalizeOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessException("请至少选择一个待支付订单");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(orderIds);
        if (uniqueIds.contains(null) || uniqueIds.size() != orderIds.size()) {
            throw new BusinessException("订单列表包含无效或重复项");
        }
        if (uniqueIds.size() > 50) {
            throw new BusinessException("单次最多支付 50 个订单");
        }
        return List.copyOf(uniqueIds);
    }

    private BatchPaymentResponse toBatchResponse(List<PaymentResponse> payments) {
        int totalAmount = payments.stream().mapToInt(payment -> payment.amount() == null ? 0 : payment.amount()).sum();
        long successCount = payments.stream().filter(payment -> STATUS_SUCCESS.equals(payment.paymentStatus())).count();
        boolean hasPending = payments.stream().anyMatch(payment -> STATUS_PENDING.equals(payment.paymentStatus()));
        String status = successCount == payments.size()
            ? STATUS_SUCCESS
            : (successCount > 0 ? "PARTIAL" : (hasPending ? STATUS_PENDING : STATUS_FAILED));
        return new BatchPaymentResponse(payments, totalAmount, status);
    }

    private String mapOrderStatusToPaymentStatus(String orderStatus) {
        if (OrderService.STATUS_PENDING_PAYMENT.equals(orderStatus)) {
            return STATUS_PENDING;
        }
        if (OrderService.STATUS_PAID.equals(orderStatus)
            || OrderService.STATUS_ACCEPTED.equals(orderStatus)
            || OrderService.STATUS_COMPLETED.equals(orderStatus)) {
            return STATUS_SUCCESS;
        }
        return STATUS_FAILED;
    }

    private record PaymentOutcome(PaymentResponse response, BusinessException error) {
    }
}
