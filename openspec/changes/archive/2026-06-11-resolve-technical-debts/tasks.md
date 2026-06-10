## 1. 依赖与配置基础设施

- [x] 1.1 `pom.xml` 添加 `spring-security-crypto` 依赖
- [x] 1.2 `pom.xml` 添加 `jjwt-api`、`jjwt-impl`、`jjwt-jackson` 依赖（版本 0.12.6）
- [x] 1.3 新建 `config/PasswordEncoderConfig.java` — 暴露 `BCryptPasswordEncoder` Bean（强度 10）
- [x] 1.4 `application.yml` 移除 `MYSQL_PASSWORD` 默认值，仅保留 `${MYSQL_PASSWORD}`
- [x] 1.5 `application.yml` 添加 `jwt.secret` 和 `app.cors.allowed-origins` 配置项
- [x] 1.6 更新 `application-local.yml.example` 补全本地开发所需配置

## 2. 密码 BCrypt 加密

- [x] 2.1 `UserService.register()` — 密码经 `BCryptPasswordEncoder.encode()` 后存储
- [x] 2.2 `UserService.login()` — 检测密码格式：`$2` 开头用 BCrypt 验证，否则明文比对 + 自动升级
- [x] 2.3 `UserService.resetForgotPassword()` — 新密码经 BCrypt 编码存储
- [x] 2.4 `MerchantService.register()` — 游客创建用户时密码 BCrypt 编码

## 3. HTTP 状态码规范化

- [x] 3.1 `BusinessException` 增加 `int httpStatus` 字段（默认 400），新增双参构造器
- [x] 3.2 `GlobalExceptionHandler` 使用 `ResponseEntity<?>` 按 `httpStatus` 设置 HTTP 状态码
- [x] 3.3 `AuthInterceptor` — 未登录抛出 `BusinessException(401, "未登录，请先登录")`
- [x] 3.4 `AuthInterceptor` — 权限不足抛出 `BusinessException(403, "权限不足，无法访问")`

## 4. CORS 安全收紧

- [x] 4.1 `WebConfig.addCorsMappings()` — `allowedOriginPatterns` 改为从配置读取白名单
- [x] 4.2 `allowedHeaders` 限制为 `Authorization, Content-Type`

## 5. JWT 令牌认证

- [x] 5.1 新建 `common/JwtUtil.java` — `generateToken(phone, role)` + `parseToken(token)` + `isTokenValid(token)`
- [x] 5.2 `dto/LoginResponse.java` — 新增 `token` 字段
- [x] 5.3 `UserService.login()` — 登录成功后生成 JWT 并填充 `LoginResponse.token`
- [x] 5.4 `UserService.register()` — 注册成功后生成 JWT 并填充响应（返回类型改为 LoginResponse）
- [x] 5.5 `UserService.resetForgotPassword()` — 重置成功后生成 JWT 并填充响应
- [x] 5.6 `AuthInterceptor.preHandle()` — 解析 `Bearer <token>` → JWT 验证 → 设置 UserContext
- [x] 5.7 `AuthInterceptor` — JWT 过期或无效返回 401
- [x] 5.8 `UserContext` — 增加 `setUser(phone, role)` 便捷方法（无需查 DB）

## 6. 前端适配

- [x] 6.1 `api/session.js` — 新增 `currentToken()`，`setSessionUser` 存储完整数据（含 token）
- [x] 6.2 `api/client.js` — 请求拦截器改发 `Authorization: Bearer <token>`（兼容旧的 phone 直传）
- [x] 6.3 `api/client.js` — 响应拦截器 401 自动清除 token 并跳转登录

## 7. 数据库迁移

- [x] 7.1 `database/schema.sql` — 演示用户密码改为 BCrypt 哈希
- [x] 7.2 新建 `database/migration-20260610-security.sql` — 说明密码字段变更为 BCrypt

## 8. 清理

- [x] 8.1 检查所有 @RequireRole 注解的接口确保 AuthInterceptor 正确拦截 — 无问题
- [x] 8.2 检查所有 Service 中 `UserContext.getUserId()` 调用是否需要适配 — 无需修改，JWT 模式下 phone 值一致
