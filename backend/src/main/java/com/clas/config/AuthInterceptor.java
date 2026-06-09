package com.clas.config;

import com.clas.common.BusinessException;
import com.clas.common.JwtUtil;
import com.clas.entity.User;
import com.clas.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthInterceptor(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            String authValue = authHeader.trim();

            if (authValue.startsWith("Bearer ")) {
                // JWT Bearer token 模式
                String token = authValue.substring(7).trim();
                if (jwtUtil.isTokenValid(token)) {
                    String phone = jwtUtil.getPhoneFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);
                    // 检查账号是否仍处于启用状态
                    User user = userMapper.selectById(phone);
                    if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
                        throw new BusinessException(401, "账号已被禁用或不存在");
                    }
                    UserContext.setUser(phone, role);
                } else {
                    throw new BusinessException(401, "登录已过期，请重新登录");
                }
            } else {
                // 向后兼容：直接传 phone 模式（过渡期）
                User user = userMapper.selectById(authValue);
                if (user != null) {
                    if (Boolean.FALSE.equals(user.getEnabled())) {
                        throw new BusinessException(401, "账号已被禁用");
                    }
                    UserContext.setUser(user);
                }
            }
        }

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            if (requireRole == null) {
                requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
            }

            if (requireRole != null) {
                User currentUser = UserContext.getUser();
                if (currentUser == null) {
                    throw new BusinessException(401, "未登录，请先登录");
                }
                String[] allowedRoles = requireRole.value();
                if (allowedRoles.length > 0) {
                    boolean hasRole = Arrays.asList(allowedRoles).contains(currentUser.getRole());
                    if (!hasRole) {
                        throw new BusinessException(403, "权限不足，无法访问");
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
