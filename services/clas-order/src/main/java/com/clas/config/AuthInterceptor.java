package com.clas.config;

import com.clas.common.BusinessException;
import com.clas.common.JwtUtil;
import com.clas.entity.User;
import com.clas.mapper.UserMapper;
import com.clas.mapper.UserRoleMapper;
import com.clas.service.SessionTouchService;
import com.clas.service.AuthPenaltyService;
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
    private final SessionTouchService sessionTouchService;
    private final AuthPenaltyService authPenaltyService;

    public AuthInterceptor(UserMapper userMapper, UserRoleMapper userRoleMapper, JwtUtil jwtUtil,
                           SessionTouchService sessionTouchService, AuthPenaltyService authPenaltyService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.userRoleMapper = userRoleMapper;
        this.sessionTouchService = sessionTouchService;
        this.authPenaltyService = authPenaltyService;
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
                String tokenSession = jwtUtil.getSessionTokenFromToken(token);
                if (tokenSession == null || !tokenSession.equals(user.getSessionToken())) {
                    throw new BusinessException(401, "账号已在其他设备登录，请重新登录");
                }
                String activeRole = jwtUtil.getRoleFromToken(token);
                UserRole identity = userRoleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, phone).eq(UserRole::getRole, activeRole));
                boolean legacyIdentity = identity == null && activeRole != null && activeRole.equals(user.getRole());
                if (!legacyIdentity && (identity == null || !"APPROVED".equals(identity.getStatus()))) {
                    throw new BusinessException(403, "当前身份尚未审核通过或已不可用");
                }
                // Administrator tokens remain bound to the legacy administrator flag; merchant and rider
                // identities may be added to an originally USER account through the approved role table.
                if ("ADMIN".equals(activeRole) && !activeRole.equals(user.getRole())) {
                    throw new BusinessException(403, "当前端口身份已失效，请重新登录");
                }
                user.setRole(activeRole);
                UserContext.setUser(user);
                if (authPenaltyService.isAccountOnlyRestricted(phone) && !isAccountInformationRequest(request)) {
                    throw new BusinessException(403, "当前账号仅保留账户信息、处罚记录和申诉入口", "ACCOUNT_ONLY_RESTRICTED");
                }
                sessionTouchService.touchActiveSession(user);
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

    private boolean isAccountInformationRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/user/logout".equals(path)
            || "/api/user/profile".equals(path)
            || "/api/user/profile/avatar".equals(path)
            || "/api/user/password".equals(path)
            || "/api/user/phone".equals(path)
            || "/api/user/phone-change/send-code".equals(path)
            || "/api/user/penalties/mine".equals(path)
            || "/api/user/appeals".equals(path)
            || "/api/user/appeals/mine".equals(path);
    }
}
