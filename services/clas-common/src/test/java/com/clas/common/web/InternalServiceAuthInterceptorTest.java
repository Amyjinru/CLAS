package com.clas.common.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clas.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalServiceAuthInterceptorTest {
    private final InternalServiceAuthInterceptor interceptor = new InternalServiceAuthInterceptor("test-internal-key");

    @Test
    void acceptsAuthenticatedServiceRequest() {
        assertTrue(interceptor.preHandle(request("clas-order", "test-internal-key"), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void rejectsRequestWithoutValidServiceCredentials() {
        assertThrows(BusinessException.class,
            () -> interceptor.preHandle(request("clas-order", "wrong-key"), new MockHttpServletResponse(), new Object()));
        assertThrows(BusinessException.class,
            () -> interceptor.preHandle(request(null, "test-internal-key"), new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest request(String service, String key) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/catalog/v1/products/1");
        if (service != null) {
            request.addHeader("X-CLAS-Service", service);
        }
        request.addHeader("X-CLAS-Internal-Key", key);
        return request;
    }
}
