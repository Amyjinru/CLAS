package com.clas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.clas.common.MerchantStatusEnum;
import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import com.clas.entity.Product;
import com.clas.mapper.MerchantAuditLogMapper;
import com.clas.mapper.FavoriteMapper;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.OrdersMapper;
import com.clas.mapper.ProductMapper;
import com.clas.mapper.RiderApplicationMapper;
import com.clas.mapper.RoleApplicationMapper;
import com.clas.mapper.UserAddressMapper;
import com.clas.mapper.UserMapper;
import com.clas.common.VerificationCodeStore;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MerchantAveragePriceTest {

    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private MerchantAuditLogMapper merchantAuditLogMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserService userService;
    @Mock
    private UserAddressMapper userAddressMapper;
    @Mock
    private OrdersMapper ordersMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private RiderApplicationMapper riderApplicationMapper;
    @Mock
    private RoleApplicationMapper roleApplicationMapper;
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private VerificationCodeStore verificationCodeStore;
    @Mock
    private AmapRouteService amapRouteService;
    @Mock
    private RecommendService recommendService;

    private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        merchantService = new MerchantService(
            merchantMapper,
            merchantAuditLogMapper,
            userService,
            userMapper,
            userAddressMapper,
            ordersMapper,
            productMapper,
            riderApplicationMapper,
            roleApplicationMapper,
            favoriteMapper,
            verificationCodeStore,
            amapRouteService,
            recommendService,
            new BCryptPasswordEncoder()
        );
    }

    @Test
    void usesProductAverageWhenCompletedOrdersBelowThreshold() {
        Merchant merchant = merchant(1L, 0);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(ordersMapper.selectList(any())).thenReturn(List.of());
        when(productMapper.selectList(any())).thenReturn(List.of(
            product(1L, 2000),
            product(1L, 4000)
        ));

        merchantService.refreshAveragePrice(1L);

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).updateById(captor.capture());
        assertEquals(3000, captor.getValue().getAveragePrice());
    }

    @Test
    void usesCompletedOrderAverageWhenThresholdReached() {
        Merchant merchant = merchant(2L, 0);
        when(merchantMapper.selectById(2L)).thenReturn(merchant);
        when(ordersMapper.selectList(any())).thenReturn(List.of(
            completedOrder(2L, 1800),
            completedOrder(2L, 2200),
            completedOrder(2L, 2000),
            completedOrder(2L, 2400),
            completedOrder(2L, 1600),
            completedOrder(2L, 2100),
            completedOrder(2L, 1900),
            completedOrder(2L, 2300),
            completedOrder(2L, 1700),
            completedOrder(2L, 2000)
        ));

        merchantService.refreshAveragePrice(2L);

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).updateById(captor.capture());
        assertEquals(2000, captor.getValue().getAveragePrice());
    }

    private Merchant merchant(Long id, Integer averagePrice) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setAveragePrice(averagePrice);
        merchant.setStatus(MerchantStatusEnum.OPEN);
        merchant.setScore(BigDecimal.ZERO);
        return merchant;
    }

    private Product product(Long merchantId, int price) {
        Product product = new Product();
        product.setMerchantId(merchantId);
        product.setPrice(price);
        product.setStatus("ON_SALE");
        return product;
    }

    private Orders completedOrder(Long merchantId, int totalPrice) {
        Orders order = new Orders();
        order.setMerchantId(merchantId);
        order.setStatus("COMPLETED");
        order.setTotalPrice(totalPrice);
        return order;
    }
}
