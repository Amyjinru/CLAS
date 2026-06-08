package com.clas.config;

import com.clas.common.BusinessException;
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

    public AuthInterceptor(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            User user = userMapper.selectById(authHeader.trim());
            if (user != null) {
                if (Boolean.FALSE.equals(user.getEnabled())) {
                    throw new BusinessException("账号已被禁用");
                }
                UserContext.setUser(user);
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
                    throw new BusinessException("未登录，请先登录");
                }
                String[] allowedRoles = requireRole.value();
                if (allowedRoles.length > 0) {
                    boolean hasRole = Arrays.asList(allowedRoles).contains(currentUser.getRole());
                    if (!hasRole) {
                        throw new BusinessException("权限不足，无法访问");
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
