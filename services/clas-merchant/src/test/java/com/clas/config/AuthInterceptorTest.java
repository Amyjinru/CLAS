package com.clas.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clas.client.IamClient;
import com.clas.common.BusinessException;
import com.clas.common.JwtUtil;
import com.clas.common.client.ServiceEndpoints;
import com.clas.common.dto.InternalValidatedUser;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestTemplate;

class AuthInterceptorTest {
    private final JwtUtil jwtUtil = new JwtUtil("catalog-auth-test-secret-must-have-32-bytes", 60_000, new MockEnvironment());

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void acceptsIamValidatedToken() {
        AuthInterceptor interceptor = new AuthInterceptor(jwtUtil, iamClient(activeUser()));
        String token = jwtUtil.generateToken("13800000000", "USER", "session-1");

        assertTrue(interceptor.preHandle(request(token), new MockHttpServletResponse(), new Object()));
        assertEquals("13800000000", UserContext.getUserId());
        assertEquals("USER", UserContext.getUser().getRole());
    }

    @Test
    void propagatesIamSessionRejection() {
        AuthInterceptor interceptor = new AuthInterceptor(jwtUtil, rejectingIamClient());
        String token = jwtUtil.generateToken("13800000000", "USER", "old-session");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> interceptor.preHandle(request(token), new MockHttpServletResponse(), new Object()));
        assertEquals(401, exception.getHttpStatus());
    }

    private IamClient iamClient(InternalValidatedUser user) {
        return new IamClient((RestTemplate) null, (ServiceEndpoints) null) {
            @Override
            public InternalValidatedUser validateToken(String token) {
                return user;
            }
        };
    }

    private IamClient rejectingIamClient() {
        return new IamClient((RestTemplate) null, (ServiceEndpoints) null) {
            @Override
            public InternalValidatedUser validateToken(String token) {
                throw new BusinessException(401, "账号已在其他设备登录，请重新登录");
            }
        };
    }

    private InternalValidatedUser activeUser() {
        return new InternalValidatedUser("13800000000", "测试用户", "USER", List.of("USER"), false);
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/product/list");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
