package com.clas.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogServiceApplicationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicCatalogReturnsOnlyOnSaleProductsAndCategories() throws Exception {
        mockMvc.perform(get("/api/product/list/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].categoryName").value("主食"));

        mockMvc.perform(get("/api/product/categories").param("merchantId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("主食"));
    }

    @Test
    void internalContractRejectsMissingOrBadServiceKey() throws Exception {
        mockMvc.perform(get("/internal/catalog/v1/products/100").param("merchantId", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/internal/catalog/v1/products/100")
                        .param("merchantId", "1")
                        .header("X-Internal-Service-Key", "bad-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("internal service authentication failed"));
    }

    @Test
    void internalContractReturnsSnapshotAndAvailabilityFailures() throws Exception {
        mockMvc.perform(get("/internal/catalog/v1/products/100")
                        .param("merchantId", "1")
                        .header("X-Internal-Service-Key", "catalog-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("牛肉饭"))
                .andExpect(jsonPath("$.data.stock").value(5));

        mockMvc.perform(post("/internal/catalog/v1/products/availability")
                        .header("X-Internal-Service-Key", "catalog-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantId\":1,\"items\":[{\"productId\":100,\"quantity\":6}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(get("/internal/catalog/v1/products/999")
                        .param("merchantId", "1")
                        .header("X-Internal-Service-Key", "catalog-test-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
