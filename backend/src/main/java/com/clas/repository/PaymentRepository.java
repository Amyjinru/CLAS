package com.clas.repository;

import com.clas.entity.Payment;
import java.util.Optional;

/**
 * 支付数据访问接口，预留 MySQL 持久化实现，便于后期替换或扩展。
 */
public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findLatestByOrderId(Long orderId);

    Optional<Payment> findSuccessfulByOrderId(Long orderId);
}
