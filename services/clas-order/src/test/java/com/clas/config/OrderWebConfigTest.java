package com.clas.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class OrderWebConfigTest {
    @Test
    void servesUploadedReviewImages() {
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(
            new StaticWebApplicationContext(), new MockServletContext());

        new OrderWebConfig(null, null).addResourceHandlers(registry);

        assertTrue(registry.hasMappingForPattern("/uploads/**"));
    }
}
