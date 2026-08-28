## ADDED Requirements

### Requirement: 登录返回 JWT 令牌
系统 SHALL 在用户登录、注册、忘记密码重置成功后返回 JWT 令牌。`LoginResponse` 中新增 `token` 字段，包含签名的 JWT 字符串。

#### Scenario: 登录成功返回 token
- **WHEN** 用户通过 `POST /api/user/login` 成功登录
- **THEN** 响应 `data.token` 包含有效的 JWT 字符串

#### Scenario: 注册成功返回 token
- **WHEN** 用户通过 `POST /api/user/register` 成功注册
- **THEN** 响应 `data.token` 包含有效 JWT，用户可直接使用无需再次登录

#### Scenario: 忘记密码重置后返回 token
- **WHEN** 用户通过 `POST /api/user/forgot-password/reset` 成功重置密码
- **THEN** 响应 `data.token` 包含有效 JWT

### Requirement: JWT 令牌格式和载荷
系统 SHALL 生成包含以下声明的 JWT：`sub`（用户 phone）、`role`（用户角色）、`iat`（签发时间）、`exp`（过期时间，24 小时）。令牌使用 HMAC-SHA256 签名，密钥从配置 `jwt.secret` 读取。

#### Scenario: JWT 包含正确载荷
- **WHEN** user 角色用户（phone=13800000001）登录
- **THEN** 生成的 JWT 解码后 `sub` 为 "13800000001"，`role` 为 "USER"，`exp` 为 24 小时后

### Requirement: AuthInterceptor 验证 JWT
系统 SHALL 在 `AuthInterceptor` 中解析 `Authorization: Bearer <token>` 请求头。验证签名和过期时间后，从 JWT 载荷提取用户信息并存入 `UserContext`，不再每次请求查 DB。

#### Scenario: 有效 Bearer token 通过认证
- **WHEN** 请求头为 `Authorization: Bearer <valid_token>`
- **THEN** UserContext 被正确设置，请求继续处理

#### Scenario: 无效 token 被拒绝
- **WHEN** 请求头为 `Authorization: Bearer <tampered_token>` 或签名无效
- **THEN** 返回 HTTP 401 和错误消息 "登录已过期，请重新登录"

#### Scenario: 过期 token 被拒绝
- **WHEN** 请求头为 `Authorization: Bearer <expired_token>`（超过 24 小时）
- **THEN** 返回 HTTP 401 和错误消息 "登录已过期，请重新登录"

#### Scenario: 缺少 Authorization 头的公开接口正常访问
- **WHEN** 请求 `GET /api/merchant/list` 且无 Authorization 头
- **THEN** 请求正常处理，UserContext 为空（未登录状态）

### Requirement: 前端存储和传递 JWT
前端 SHALL 将登录响应中的 `token` 字段存入 `localStorage`。所有 API 请求通过 `client.js` 拦截器自动携带 `Authorization: Bearer <token>` 头。

#### Scenario: 登录后 token 被持久化
- **WHEN** 用户成功登录
- **THEN** `localStorage.clas_user` 中包含 `token` 字段

#### Scenario: API 请求自动携带 Bearer token
- **WHEN** 已登录用户发起任意 `/api/*` 请求
- **THEN** 请求头包含 `Authorization: Bearer <token>`

#### Scenario: 退出登录清除 token
- **WHEN** 用户点击退出
- **THEN** `localStorage.clas_user` 被移除，后续请求不再携带 Authorization 头
