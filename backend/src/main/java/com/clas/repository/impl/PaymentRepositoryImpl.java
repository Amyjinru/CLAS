package com.clas.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.entity.Payment;
import com.clas.mapper.PaymentMapper;
import com.clas.repository.PaymentRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentMapper paymentMapper;

    public PaymentRepositoryImpl(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    @Override
    public Payment save(Payment payment) {
        // 支付流水创建后还会更新状态，因此同一个 save 同时支持插入和更新。
        if (payment.getId() == null) {
            paymentMapper.insert(payment);
        } else {
            paymentMapper.updateById(payment);
        }
        return payment;
    }

    @Override
    public Optional<Payment> findLatestByOrderId(Long orderId) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
            .eq(Payment::getOrderId, orderId)
            .orderByDesc(Payment::getCreateTime)
            .orderByDesc(Payment::getId)
            .last("LIMIT 1"));
        return Optional.ofNullable(payment);
    }

    @Override
    public Optional<Payment> findSuccessfulByOrderId(Long orderId) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
            .eq(Payment::getOrderId, orderId)
            .eq(Payment::getStatus, "SUCCESS")
            .orderByDesc(Payment::getCreateTime)
            .orderByDesc(Payment::getId)
            .last("LIMIT 1"));
        return Optional.ofNullable(payment);
    }
}
