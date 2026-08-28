## ADDED Requirements

### Requirement: 未登录返回 401
系统 SHALL 在 `AuthInterceptor` 检测到请求需要认证但未提供有效凭据时，返回 HTTP 401 Unauthorized。

#### Scenario: 未登录访问需认证接口
- **WHEN** 无 Authorization 头的请求访问 `GET /api/cart/list/1`（`@RequireRole("USER")`）
- **THEN** HTTP 响应状态码为 401，body 中 `code` 为 401，`message` 为 "未登录，请先登录"

#### Scenario: 过期 token 访问需认证接口
- **WHEN** 携带已过期 JWT 的请求访问需认证接口
- **THEN** HTTP 响应状态码为 401

### Requirement: 权限不足返回 403
系统 SHALL 在 `AuthInterceptor` 检测到用户角色不满足 `@RequireRole` 要求时，返回 HTTP 403 Forbidden。

#### Scenario: USER 角色访问 ADMIN 接口
- **WHEN** USER 角色用户携带有效 token 访问 `GET /api/admin/dashboard`
- **THEN** HTTP 响应状态码为 403，body 中 `code` 为 403，`message` 为 "权限不足，无法访问"

#### Scenario: MERCHANT 角色访问 ADMIN 接口
- **WHEN** MERCHANT 角色用户访问 `GET /api/admin/users`
- **THEN** HTTP 响应状态码为 403

### Requirement: 业务错误保持 400
系统 SHALL 在业务逻辑错误时保持返回 HTTP 400 Bad Request。

#### Scenario: 参数校验失败返回 400
- **WHEN** 请求参数不满足 `@Valid` 校验（如 price 为负）
- **THEN** HTTP 响应状态码为 400

#### Scenario: 业务规则冲突返回 400
- **WHEN** 重复入驻商家
- **THEN** HTTP 响应状态码为 400，`message` 为 "每个用户只能入驻一个商家"

### Requirement: BusinessException 支持 HTTP 状态码
`BusinessException` SHALL 支持可选的 HTTP 状态码字段。`GlobalExceptionHandler` SHALL 使用 `ResponseEntity<?>` 按异常中的状态码设置 HTTP 响应状态。

#### Scenario: 默认状态码为 400
- **WHEN** `throw new BusinessException("错误")` 不带状态码
- **THEN** HTTP 响应状态码为 400

#### Scenario: 指定状态码的异常
- **WHEN** `throw new BusinessException(401, "未登录")` 
- **THEN** HTTP 响应状态码为 401
