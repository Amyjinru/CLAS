package com.clas.config;

import com.clas.common.BusinessException;
import com.clas.common.JwtUtil;
import com.clas.entity.User;
import com.clas.mapper.UserMapper;
import com.clas.mapper.UserRoleMapper;
import com.clas.entity.UserRole;
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
    private final UserRoleMapper userRoleMapper;

    public AuthInterceptor(UserMapper userMapper, UserRoleMapper userRoleMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
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
                String token = authValue.substring(7).trim();
                if (!jwtUtil.isTokenValid(token)) {
                    throw new BusinessException(401, "登录已过期，请重新登录");
                }
                String phone = jwtUtil.getPhoneFromToken(token);
                User user = userMapper.selectById(phone);
                if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
                    throw new BusinessException(401, "账号已被禁用或不存在");
                }
                String activeRole = jwtUtil.getRoleFromToken(token);
                boolean roleGranted = userRoleMapper.exists(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, phone).eq(UserRole::getRole, activeRole));
                // 兼容尚未执行身份表迁移的旧账号；迁移后每次请求均由 user_role 校验。
                // USER 是每个账号永久拥有的基础端口；兼容迁移前尚未回填 user_role 的旧会话。
                if ("USER".equals(activeRole)) roleGranted = true;
                if (!roleGranted && activeRole.equals(user.getRole())) roleGranted = true;
                if (!roleGranted) throw new BusinessException(403, "当前端口身份已失效，请重新登录");
                // 令牌只证明登录身份；当前端口必须仍属于数据库中的已授予身份。
                user.setRole(activeRole);
                UserContext.setUser(user);
            } else {
                throw new BusinessException(401, "未登录，请先登录");
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
