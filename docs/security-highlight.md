# CLAS 安全亮点说明

## 概述

CLAS 平台在课程设计项目中引入了五项安全加固，从密码存储、身份认证、HTTP 响应规范、跨域控制到配置安全，实现了演示级安全闭环。

## 1. BCrypt 密码哈希存储

### 改造前
```java
// 旧：明文存储（安全隐患）
user.setPassword(request.password());
```

### 改造后
```java
// 新：BCrypt 哈希存储
String hashed = passwordEncoder.encode(request.password());
user.setPassword(hashed);
```

### 兼容策略
- 新注册/重置密码：直接 BCrypt 哈希存储
- 旧明文密码：登录时自动检测，非 `$2` 前缀的密码在验证通过后自动升级为 BCrypt 哈希

### 核心代码位置
- `backend/src/main/java/com/clas/service/UserService.java` — `login()`, `register()`, `resetPassword()`
- `backend/src/main/java/com/clas/config/SecurityConfig.java` — BCryptPasswordEncoder Bean

## 2. JWT 令牌认证

### 改造前
```
Authorization: 13800000001  (直接传手机号)
```

### 改造后
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...  (标准 JWT 令牌)
```

### JWT 特性
| 特性 | 说明 |
|------|------|
| 签名算法 | HMAC-SHA256 |
| 密钥来源 | `jwt.secret` 配置项（32字节+） |
| 过期时间 | 24 小时 |
| 令牌内容 | `sub`（手机号）、`role`（角色）、`exp`（过期时间） |

### 核心代码位置
- `backend/src/main/java/com/clas/config/JwtUtils.java` — 令牌生成与验证
- `backend/src/main/java/com/clas/config/AuthInterceptor.java` — 请求拦截与身份提取
- `frontend/src/utils/session.js` — 前端令牌存储与发送

## 3. HTTP 状态码规范化

| 场景 | 改造前 | 改造后 | HTTP 状态码 |
|------|--------|--------|-------------|
| 未登录 | 200 + "未登录" | 统一 401 | 401 Unauthorized |
| 权限不足 | 200 + "无权限" | 统一 403 | 403 Forbidden |
| 业务错误 | 200 + 错误信息 | 统一 400 | 400 Bad Request |
| 正常响应 | 200 + 数据 | 保持不变 | 200 OK |

### 核心代码位置
- `backend/src/main/java/com/clas/common/BusinessException.java` — 支持 HTTP 状态码
- `backend/src/main/java/com/clas/common/GlobalExceptionHandler.java` — 统一异常处理

## 4. CORS 跨域白名单

### 改造前
```java
// 旧：允许所有来源（安全风险）
configuration.addAllowedOriginPattern("*");
```

### 改造后
```yaml
# 新：配置化白名单
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}
```

### 核心代码位置
- `backend/src/main/java/com/clas/config/WebConfig.java` — CORS 配置
- `backend/src/main/resources/application.yml` — 白名单配置

## 5. 配置文件安全

### 改造前
```yaml
spring:
  datasource:
    password: clas2026!  # 明文硬编码密码
```

### 改造后
```yaml
spring:
  datasource:
    password: ${MYSQL_PASSWORD}  # 强制环境变量注入，无默认值
```

### 关键配置项
| 配置项 | 来源 | 说明 |
|--------|------|------|
| `MYSQL_PASSWORD` | 环境变量 | 数据库密码 |
| `JWT_SECRET` | 环境变量 | JWT 签名密钥 |
| `AMAP_WEB_SERVICE_KEY` | 环境变量 | 高德地图 Key |
| `CORS_ALLOWED_ORIGINS` | 环境变量 | CORS 白名单 |
| `DASHSCOPE_API_KEY` | 环境变量 | AI 内容审核 Key |

## 6. 前端安全适配

| 改动 | 文件 | 说明 |
|------|------|------|
| JWT 令牌存储 | `frontend/src/utils/session.js` | Token 持久化到 localStorage |
| 请求拦截器 | `frontend/src/api/client.js` | 自动附加 `Authorization: Bearer <token>` |
| 401/403 处理 | `frontend/src/api/client.js` | 令牌过期自动跳转登录页 |

## 7. 数据库迁移

安全加固相关的数据库迁移：
- `database/migration-20260610-security.sql` — 验证密码列长度，记录升级说明
- 密码字段 `VARCHAR(255)` 无需 DDL 变更，足够容纳 BCrypt 哈希（固定 60 字符）

## 8. 测试验证

```
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- 登录流程正常返回 JWT 令牌
- BCrypt 密码验证正确
- 401/403 状态码正确返回
- CORS 白名单生效

## 9. 课程演示价值

作为软件工程课程设计项目，CLAS 的安全加固体现了以下知识点：
1. **密码学基础**：BCrypt 哈希算法的加盐机制与抗暴力破解特性
2. **Web 安全**：JWT 无状态认证、Bearer Token 标准格式
3. **HTTP 协议**：状态码语义化设计（401/403/400 区分）
4. **安全编码**：敏感配置环境变量注入、CORS 最小权限原则
5. **渐进式升级**：旧密码自动升级机制，兼容已有数据
