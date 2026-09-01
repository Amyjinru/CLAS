package com.clas.config;

import com.clas.client.IamClient;
import com.clas.common.BusinessException;
import com.clas.common.JwtUtil;
import com.clas.common.dto.InternalUserAuthState;
import com.clas.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Validates JWT claims against IAM; this service never reads IAM-owned tables. */
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
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            if (!authHeader.trim().startsWith("Bearer ")) throw new BusinessException(401, "未登录，请先登录");
            String token = authHeader.trim().substring(7).trim();
            if (!jwtUtil.isTokenValid(token)) throw new BusinessException(401, "登录已过期，请重新登录");
            InternalUserAuthState state = iamClient.getAuthState(jwtUtil.getPhoneFromToken(token));
            validateState(token, state);
            UserContext.setUser(toUser(state, jwtUtil.getRoleFromToken(token)));
            if (state.accountOnlyRestricted()) {
                throw new BusinessException(403, "当前账号仅保留账户信息、处罚记录和申诉入口", "ACCOUNT_ONLY_RESTRICTED");
            }
        }
        enforceRequiredRole(handler);
        return true;
    }

    private void validateState(String token, InternalUserAuthState state) {
        if (state == null || Boolean.FALSE.equals(state.enabled())) throw new BusinessException(401, "账号已被禁用或不存在");
        String tokenSession = jwtUtil.getSessionTokenFromToken(token);
        if (tokenSession == null || !tokenSession.equals(state.sessionToken())) {
            throw new BusinessException(401, "账号已在其他设备登录，请重新登录");
        }
        String activeRole = jwtUtil.getRoleFromToken(token);
        if (activeRole == null || !state.approvedOrLegacyRoles().contains(activeRole)) {
            throw new BusinessException(403, "当前身份尚未审核通过或已不可用");
        }
        if ("ADMIN".equals(activeRole) && !activeRole.equals(state.primaryRole())) {
            throw new BusinessException(403, "当前端口身份已失效，请重新登录");
        }
    }

    private User toUser(InternalUserAuthState state, String activeRole) {
        User user = new User();
        user.setPhone(state.userId()); user.setUsername(state.username()); user.setEnabled(state.enabled());
        user.setSessionToken(state.sessionToken()); user.setRole(activeRole); user.setRoles(state.approvedOrLegacyRoles());
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
