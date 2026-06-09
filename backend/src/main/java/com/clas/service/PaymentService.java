package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.dto.PaymentRequest;
import com.clas.dto.PaymentResponse;
import com.clas.entity.Orders;
import com.clas.entity.Payment;
import com.clas.mapper.OrdersMapper;
import com.clas.repository.PaymentRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String PAY_METHOD_FAIL_MOCK = "FAIL_MOCK";

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final OrdersMapper ordersMapper;

    public PaymentService(
        PaymentRepository paymentRepository,
        OrderService orderService,
        OrdersMapper ordersMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.ordersMapper = ordersMapper;
    }

    @Transactional
    public PaymentResponse mockPay(PaymentRequest request) {
        Orders order = orderService.requireUserOrder(request.orderId(), request.userId());
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
                if (!OrderService.STATUS_PAID.equals(order.getStatus())) {
                    order.setStatus(OrderService.STATUS_PAID);
                    ordersMapper.updateById(order);
                }
                return PaymentResponse.from(payment, order.getStatus());
            })
            .or(() -> paymentRepository.findLatestByOrderId(order.getId())
                .filter(payment -> STATUS_FAILED.equals(payment.getStatus())
                    && normalizePayMethod(request.payMethod()).equals(payment.getPayMethod()))
                .map(payment -> PaymentResponse.from(payment, order.getStatus())))
            .orElseGet(() -> processPendingPayment(order, request));
    }

    public PaymentResponse getPaymentStatus(Long orderId) {
        Orders order = orderService.requireOrder(orderId);
        return paymentStatusForOrder(orderId, order);
    }

    public PaymentResponse getPaymentStatus(Long orderId, String userId) {
        Orders order = orderService.requireUserOrder(orderId, userId);
        return paymentStatusForOrder(orderId, order);
    }

    private PaymentResponse processPendingPayment(Orders order, PaymentRequest request) {
        String payMethod = normalizePayMethod(request.payMethod());

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(request.userId());
        payment.setAmount(order.getTotalPrice());
        payment.setPayMethod(payMethod);
        payment.setStatus(STATUS_PENDING);
        payment.setCreateTime(LocalDateTime.now());
        paymentRepository.save(payment);

        try {
            Thread.sleep(1200);
            if (PAY_METHOD_FAIL_MOCK.equals(payMethod)) {
                payment.setStatus(STATUS_FAILED);
                paymentRepository.save(payment);
                return PaymentResponse.from(payment, order.getStatus());
            }

            payment.setStatus(STATUS_SUCCESS);
            paymentRepository.save(payment);

            orderService.deductStockForPayment(order.getId());
            orderService.markCouponUsed(order.getId());
            order.setStatus(OrderService.STATUS_PAID);
            ordersMapper.updateById(order);
            return PaymentResponse.from(payment, order.getStatus());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            payment.setStatus(STATUS_FAILED);
            paymentRepository.save(payment);
            throw new BusinessException("模拟支付被中断");
        }
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
}
