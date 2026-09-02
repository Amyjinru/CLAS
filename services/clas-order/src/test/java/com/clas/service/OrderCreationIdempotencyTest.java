package com.clas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clas.dto.CreateOrderRequest;
import com.clas.dto.OrderResponse;
import com.clas.entity.OrderItem;
import com.clas.entity.Orders;
import com.clas.mapper.CartMapper;
import com.clas.mapper.OrderItemMapper;
import com.clas.mapper.OrdersMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderCreationIdempotencyTest {
    @Test
    void 相同创建幂等键应复用已有订单且不再次调用下游服务() {
        OrdersMapper ordersMapper = mock(OrdersMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        CartMapper cartMapper = mock(CartMapper.class);

        Orders existing = new Orders();
        existing.setId(501L);
        existing.setUserId("13345678900");
        existing.setClientRequestKey("create-501");
        existing.setMerchantNameSnapshot("Snapshot Merchant");
        existing.setMerchantLogoSnapshot("https://example.test/merchant.png");
        OrderItem item = new OrderItem();
        item.setOrderId(501L);
        item.setProductId(701L);
        item.setProductNameSnapshot("Snapshot Product");
        item.setProductImageSnapshot("https://example.test/product.png");
        when(ordersMapper.findByUserIdAndClientRequestKey("13345678900", "create-501")).thenReturn(existing);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        OrderService service = new OrderService(
            ordersMapper, orderItemMapper, cartMapper, null, null,
            null, null, null, null, null, null
        );

        OrderResponse response = service.create(new CreateOrderRequest("13345678900", 1L), "create-501");

        assertThat(response.order().getId()).isEqualTo(501L);
        assertThat(response.items()).extracting(OrderItem::getProductId).containsExactly(701L);
        assertThat(response.merchantName()).isEqualTo("Snapshot Merchant");
        assertThat(response.products()).singleElement().satisfies(product -> {
            assertThat(product.name()).isEqualTo("Snapshot Product");
            assertThat(product.image()).isEqualTo("https://example.test/product.png");
        });
        verify(cartMapper, never()).selectList(any());
        verify(ordersMapper, never()).insert(any(Orders.class));
    }
}
