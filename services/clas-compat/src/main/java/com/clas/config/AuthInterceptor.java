package com.clas.config;

import com.clas.client.IamClient;
import com.clas.common.BusinessException;
import com.clas.common.JwtUtil;
import com.clas.common.dto.InternalValidatedUser;
import com.clas.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Uniform external-token validation through IAM; this service never reads IAM-owned tables. */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final IamClient iamClient;

    public AuthInterceptor(JwtUtil jwtUtil, IamClient iamClient) {
        this.jwtUtil = jwtUtil;
        this.iamClient = iamClient;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            if (!authorization.trim().startsWith("Bearer ")) throw new BusinessException(401, "未登录，请先登录");
            String token = authorization.trim().substring(7).trim();
            if (!jwtUtil.isTokenValid(token)) throw new BusinessException(401, "登录已过期，请重新登录");
            InternalValidatedUser user = iamClient.validateToken(token);
            if (user == null) throw new BusinessException(401, "账号已被禁用或不存在");
            UserContext.setUser(toUser(user));
            if (user.accountOnlyRestricted()) {
                throw new BusinessException(403, "当前账号仅保留账户信息、处罚记录和申诉入口", "ACCOUNT_ONLY_RESTRICTED");
            }
        }
        enforceRequiredRole(handler);
        return true;
    }

    private User toUser(InternalValidatedUser validated) {
        User user = new User();
        user.setPhone(validated.userId()); user.setUsername(validated.username()); user.setEnabled(true);
        user.setRole(validated.activeRole()); user.setRoles(validated.roles());
        return user;
    }

    private void enforceRequiredRole(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) return;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        if (requireRole == null) return;
        User currentUser = UserContext.getUser();
        if (currentUser == null) throw new BusinessException(401, "未登录，请先登录");
        if (requireRole.value().length > 0 && !Arrays.asList(requireRole.value()).contains(currentUser.getRole())) {
            throw new BusinessException(403, "权限不足，无法访问");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
