package com.clas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clas.client.MerchantClient;
import com.clas.common.BusinessException;
import com.clas.common.GlobalExceptionHandler;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.entity.Product;
import com.clas.mapper.ProductCategoryMapper;
import com.clas.mapper.ProductMapper;
import com.clas.service.ProductService;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class ProductControllerContractTest {
    @Test
    void listsOnlyCatalogProductsThroughPublicApiContract() {
        Product product = new Product();
        product.setId(11L);
        product.setName("测试商品");
        ProductController controller = new ProductController(productService(product), merchantClient());

        Result<List<Product>> result = controller.list(3L);

        assertEquals(200, result.code());
        assertEquals(List.of(11L), result.data().stream().map(Product::getId).toList());
    }

    @Test
    void mapsInvalidPublicCategoryRequestToClientError() {
        ProductController controller = new ProductController(productService(new Product()), merchantClient());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> controller.createCategory(new ProductController.CategoryRequest(null, " ", 0)));
        ResponseEntity<Result<Void>> response = new GlobalExceptionHandler().handleBusinessException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(400, response.getBody().code());
    }

    private ProductService productService(Product product) {
        ProductMapper productMapper = (ProductMapper) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {ProductMapper.class},
            (proxy, method, args) -> "selectList".equals(method.getName()) ? List.of(product) : defaultValue(method.getReturnType())
        );
        ProductCategoryMapper categoryMapper = (ProductCategoryMapper) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {ProductCategoryMapper.class},
            (proxy, method, args) -> defaultValue(method.getReturnType())
        );
        return new ProductService(productMapper, categoryMapper, merchantClient());
    }

    private MerchantClient merchantClient() {
        return new MerchantClient((RestTemplate) null, (ServiceEndpoints) null) {
            @Override
            public Long getCurrentMerchantId() {
                return 3L;
            }
        };
    }

    private Object defaultValue(Class<?> type) {
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == boolean.class) {
            return false;
        }
        return null;
    }
}
