package com.clas.common.web;

import com.clas.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Restricts internal contracts to callers that share the deployment service key. */
@Component
public class InternalServiceAuthInterceptor implements HandlerInterceptor {
    private final byte[] expectedKey;

    public InternalServiceAuthInterceptor(@Value("${clas.internal-api-key:}") String internalApiKey) {
        this.expectedKey = internalApiKey == null ? new byte[0] : internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String caller = request.getHeader("X-CLAS-Service");
        String suppliedKey = request.getHeader("X-CLAS-Internal-Key");
        byte[] supplied = suppliedKey == null ? new byte[0] : suppliedKey.getBytes(StandardCharsets.UTF_8);
        if (caller == null || caller.isBlank() || expectedKey.length == 0 || !MessageDigest.isEqual(expectedKey, supplied)) {
            throw new BusinessException(401, "内部服务身份验证失败", "INTERNAL_UNAUTHORIZED");
        }
        return true;
    }
}
