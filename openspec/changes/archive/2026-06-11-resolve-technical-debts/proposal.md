## Why

CLAS 项目经过 6 个开发阶段后功能趋于完整，但测试报告中记录了 7 项已知技术债务，其中密码明文存储和无令牌过期机制是安全硬伤，错误码不统一导致前端无法区分认证失败与业务错误。在继续叠加新功能之前，系统性地清偿这些债务，避免后续重构成本指数增长。

## What Changes

- **密码加密**: 引入 BCrypt 哈希存储，兼容已有明文密码的渐进式升级
- **JWT 令牌认证**: 替换 Authorization=phone 模式，支持令牌过期和标准 Bearer 格式
- **HTTP 状态码规范化**: 未登录返回 401，权限不足返回 403，与业务错误 400 区分
- **CORS 收紧**: 从 `*` 全放通收敛为配置化的白名单
- **配置文件安全**: 移除数据库密码明文默认值，强制使用环境变量
- **数据库迁移**: 新增迁移脚本处理已有数据的密码升级

## Capabilities

### New Capabilities

- `password-encryption`: 用户密码 BCrypt 哈希存储与验证，兼容旧明文密码自动升级
- `jwt-authentication`: JWT 令牌生成、验证、过期管理，替代简单的 phone 传参认证
- `http-status-codes`: 统一 HTTP 状态码体系，区分 401 未认证、403 无权限、400 业务错误
- `cors-security`: CORS 白名单配置化，限制允许的源和方法
- `config-security`: 移除配置文件中的敏感默认值，强制环境变量注入

### Modified Capabilities

<!-- 无已有 spec 需要修改，全部为新增能力 -->

## Impact

- **后端依赖**: 新增 `spring-security-crypto`（仅 crypto 模块）、`jjwt-api/impl/jackson`
- **UserService**: `login()` 返回 JWT token，`register()`/`resetPassword()` 存储 BCrypt 哈希
- **MerchantService**: `register()` 中游客创建用户时同步改造
- **AuthInterceptor**: 从查 DB 验证 phone 改为解析 JWT Bearer token
- **LoginResponse DTO**: 新增 `token` 字段
- **BusinessException / GlobalExceptionHandler**: 增加 HTTP 状态码支持
- **WebConfig**: CORS 改为配置化白名单
- **application.yml**: 移除 `MYSQL_PASSWORD` 默认值
- **前端 session.js / client.js**: 存储 JWT token，发送 `Authorization: Bearer <token>`
- **数据库 schema.sql**: 演示密码改为 BCrypt 哈希
- **数据库迁移脚本**: 新建 migration 处理已有明文密码升级
