package com.clas.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class CatalogWebConfigTest {
    @Test
    void mapsUploadedProductImagesToTheWorkingDirectoryUploadsFolder() {
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(
            new StaticWebApplicationContext(), new MockServletContext());

        new CatalogWebConfig(null, null)
            .addResourceHandlers(registry);

        assertTrue(registry.hasMappingForPattern("/uploads/**"));
    }
}
