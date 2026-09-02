package com.clas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clas.common.BusinessException;
import com.clas.common.Result;
import com.clas.dto.StockChangeRequest;
import com.clas.entity.Product;
import com.clas.mapper.GroupDealMapper;
import com.clas.mapper.ProductMapper;
import com.clas.service.InternalCatalogProductService;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InternalCatalogControllerContractTest {
    @Test
    void exposesProductQueryAndStockContracts() {
        Product product = new Product();
        product.setId(7L);
        product.setStock(3);
        AtomicInteger restoredQuantity = new AtomicInteger();
        ProductMapper mapper = productMapper(product, restoredQuantity);
        InternalCatalogController controller = new InternalCatalogController(
            new InternalCatalogProductService(mapper, noOpGroupDealMapper()), mapper
        );

        Result<Product> one = controller.getProduct(7L);
        Result<List<Product>> batch = controller.getProductsBatch("7, 8");
        Result<Boolean> deducted = controller.deductProductStock(7L, new StockChangeRequest(2));
        Result<Boolean> insufficient = controller.deductProductStock(7L, new StockChangeRequest(4));
        Result<Void> restored = controller.restoreProductStock(7L, new StockChangeRequest(2));

        assertEquals(200, one.code());
        assertEquals(7L, one.data().getId());
        assertEquals(List.of(7L, 8L), batch.data().stream().map(Product::getId).toList());
        assertTrue(deducted.data());
        assertFalse(insufficient.data());
        assertEquals(200, restored.code());
        assertEquals(2, restoredQuantity.get());
    }

    @Test
    void rejectsInvalidDeductionAndDoesNotRestoreNonPositiveQuantity() {
        AtomicInteger restoredQuantity = new AtomicInteger();
        ProductMapper mapper = productMapper(new Product(), restoredQuantity);
        InternalCatalogController controller = new InternalCatalogController(
            new InternalCatalogProductService(mapper, noOpGroupDealMapper()), mapper
        );

        BusinessException exception = assertThrows(BusinessException.class,
            () -> controller.deductProductStock(7L, new StockChangeRequest(0)));
        controller.restoreProductStock(7L, new StockChangeRequest(0));

        assertEquals(400, exception.getHttpStatus());
        assertEquals(0, restoredQuantity.get());
        assertThrows(BusinessException.class, () -> controller.getProductsBatch("7,not-a-number"));
    }

    private ProductMapper productMapper(Product product, AtomicInteger restoredQuantity) {
        return (ProductMapper) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] {ProductMapper.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "selectById" -> product;
                case "selectBatchIds" -> products((List<Long>) args[0]);
                case "deductStock" -> ((Integer) args[1]) <= 3 ? 1 : 0;
                case "restoreStock" -> {
                    restoredQuantity.set((Integer) args[1]);
                    yield 1;
                }
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private List<Product> products(List<Long> ids) {
        List<Product> products = new ArrayList<>();
        for (Long id : ids) {
            Product product = new Product();
            product.setId(id);
            products.add(product);
        }
        return products;
    }

    private GroupDealMapper noOpGroupDealMapper() {
        return (GroupDealMapper) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {GroupDealMapper.class},
            (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return 0;
    }
}
