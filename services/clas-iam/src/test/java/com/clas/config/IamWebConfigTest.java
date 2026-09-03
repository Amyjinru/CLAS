package com.clas.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class IamWebConfigTest {
    @Test
    void servesUploadedAvatars() {
        StaticWebApplicationContext context = new StaticWebApplicationContext();
        context.setServletContext(new MockServletContext());
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(context, new MockServletContext());

        new IamWebConfig(null, null).addResourceHandlers(registry);

        assertTrue(registry.hasMappingForPattern("/uploads/**"));
    }
}
