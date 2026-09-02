package com.clas.service;

import com.clas.client.OrderClient;
import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.entity.Orders;
import org.springframework.stereotype.Service;

/** compat 侧订单读取与鉴权，一律走 clas-order 内部查询 API。 */
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

    private final OrderClient orderClient;

    public OrderService(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    public Orders requireOrder(Long orderId) {
        Orders order = orderClient.getOrder(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    public Orders requireUserOrder(Long orderId, String userId) {
        Orders order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("只能操作本人订单", DomainErrorCode.AUTH_FORBIDDEN);
        }
        return order;
    }

    public Orders requireMerchantOrder(Long orderId, Long merchantId) {
        Orders order = requireOrder(orderId);
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException("只能操作本店订单", DomainErrorCode.AUTH_FORBIDDEN);
        }
        return order;
    }
}
