## Context

CLAS 当前认证方案为 `Authorization: <phone>` 直接传手机号，密码明文存储。项目已过 MVP 阶段，需要在叠加新功能前清偿安全债务。修改涉及认证体系全面升级：BCrypt 密码哈希 + JWT 令牌 + HTTP 状态码规范化 + CORS 收紧。

**当前架构**:
```
┌─────────────────────────────────────────────────────┐
│  Frontend (localStorage.phone → Authorization:phone)│
├─────────────────────────────────────────────────────┤
│  AuthInterceptor → UserMapper.selectById(phone)      │
│                   → UserContext.setUser(user)         │
│                   → @RequireRole check               │
├─────────────────────────────────────────────────────┤
│  UserService.login(): password.equals(dbPassword)    │
│  UserService.register(): user.setPassword(plaintext) │
└─────────────────────────────────────────────────────┘
```

**目标架构**:
```
┌──────────────────────────────────────────────────┐
│  Frontend (localStorage.token → Bearer <token>)   │
├──────────────────────────────────────────────────┤
│  AuthInterceptor → JwtUtil.verify(token)          │
│                   → UserContext.setUser(jwtClaims) │
│                   → @RequireRole check            │
├──────────────────────────────────────────────────┤
│  UserService.login(): BCrypt.matches(pw, hash)    │
│  UserService.register(): BCrypt.encode(pw)        │
└──────────────────────────────────────────────────┘
```

## Goals / Non-Goals

**Goals:**
- 密码使用 BCrypt 哈希存储，兼容已有明文密码的自动升级
- JWT Bearer token 替代 phone 传参，支持 24h 过期
- HTTP 401/403/400 状态码按语义正确区分
- CORS 限制为配置化白名单
- 移除 yml 中的数据库密码明文默认值

**Non-Goals:**
- 不引入 Spring Security 全量框架（保持自定义 AuthInterceptor）
- 不实现 token 刷新 (refresh token) 机制
- 不改变现有 API 路径和接口契约（除 LoginResponse 新增 token 字段外）
- 不修改价格以分为单位的设计（这是业务决策）
- 不改变 getCurrentMerchantId 的 selectOne 逻辑（已有唯一约束保证）

## Decisions

### 1. 仅引入 spring-security-crypto，不引入全量 Security

**选择**: `org.springframework.security:spring-security-crypto`，仅取 `BCryptPasswordEncoder`

**替代方案**: 
- 引入全量 `spring-boot-starter-security` → 会强制表单登录和 CSRF，与现有自定义认证冲突，需大量排除配置
- 引入第三方库 `org.mindrot:jbcrypt` → 功能相同但生态不如 Spring Crypto

**理由**: spring-security-crypto 是 Spring 生态标准，仅 1 个类，零配置冲突。

### 2. 渐进式密码迁移（非一次性批量升级）

**选择**: 登录时检测密码格式，明文密码验证通过后自动 re-hash

**替代方案**:
- 写脚本批量 BCrypt 哈希所有现有密码 → 简单但需要停机维护
- 直接强制所有用户重置密码 → 体验差

**理由**: 无需停机，用户无感知。`$2a$` 前缀是 BCrypt 的天然标识。

### 3. JWT 库选择 jjwt

**选择**: `io.jsonwebtoken:jjwt-api:0.12.6` + `jjwt-impl` + `jjwt-jackson`

**替代方案**:
- `com.auth0:java-jwt` → 也很流行，但 jjwt 更轻量
- 手写 HMAC-SHA256 → 不安全，易出错

**理由**: jjwt 是 Java 生态最流行的 JWT 库，API 简洁，支持 Builder/Fluent 模式。

### 4. JWT 不存 DB，完全无状态

**选择**: JWT 载荷包含 `{sub, role}`，AuthInterceptor 仅验证签名不查 DB

**替代方案**:
- JWT + Redis 黑名单 → 增加复杂度，当前阶段不需要主动失效
- JWT + 查 DB 获取最新角色 → 每次请求仍有 DB 开销

**理由**: 24h 短过期降低风险，角色变更在下一次登录生效是可接受的折中。

### 5. BusinessException 增加 HTTP 状态码

**选择**: 添加 `int httpStatus` 字段，构造器重载。`GlobalExceptionHandler` 使用 `ResponseEntity.status(httpStatus).body(result)`

```java
public class BusinessException extends RuntimeException {
    private final int httpStatus;
    public BusinessException(String message) { this(400, message); }
    public BusinessException(int httpStatus, String message) { ... }
}
```

### 6. CORS 配置来源

**选择**: `application.yml` 中 `app.cors.allowed-origins` 列表，环境变量全覆盖

**开发默认值**: `http://localhost:5173, http://127.0.0.1:5173`

## Risks / Trade-offs

- **[风险] JWT 密钥泄露** → 将 `jwt.secret` 默认值仅设为开发用途，生产环境通过环境变量注入强随机密钥
- **[风险] JWT 过期用户突然被登出** → 选择 24h 过期（覆盖一个自然日的使用），并在前端 401 拦截器中自动跳转登录页
- **[风险] BCrypt 验证增加登录耗时** → BCrypt 强度 10 单次验证约 100ms，在可接受范围
- **[风险] 前端 token 存储仍用 localStorage**（有 XSS 风险）→ 这属于安全问题但非当前债务项，httpOnly cookie 方案留待后续

## Migration Plan

1. **部署顺序**: 后端先部署（兼容旧前端 phone 传参和新 JWT），前端再部署
2. **向后兼容**: AuthInterceptor 先尝试 Bearer token 解析，失败时回退到 phone 直传（过渡期）
3. **回滚**: 如出现严重问题，git revert 到变更前 commit，密码字段保留 BCrypt 格式（不可逆但可重新部署旧版代码+更新 schema.sql 明文密码）
4. **数据库迁移**: 新建 `database/migration-20260610-security.sql`，包含 BCrypt 相关列的说明（不需要 DDL 变更，因 password 字段已是 VARCHAR(255)）
