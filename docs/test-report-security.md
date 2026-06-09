# CLAS 安全加固手动测试报告

> 测试范围：BCrypt 密码加密、JWT 认证、HTTP 状态码规范化、CORS 收紧、配置文件安全、前端适配、自动部署
> 测试方式：curl 命令行 + 浏览器 + 数据库查询

---

## 一、密码 BCrypt 加密

### 1.1 新用户注册密码为 BCrypt 哈希

```bash
curl -s -X POST http://8.141.112.182/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"13900001111","username":"testuser","password":"Abc123!","confirmPassword":"Abc123!","code":"000000"}'
```

**验证**: 登录 MySQL 查询 `SELECT password FROM user WHERE phone='13900001111'`，密码应为 `$2$` 开头。

- [ ] 密码字段以 `$2` 开头（BCrypt 格式）

### 1.2 正确密码登录成功

```bash
curl -s -X POST http://8.141.112.182/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13900001111","password":"Abc123!"}'
```

- [ ] 返回 code=200，data 包含 user 对象和 token 字段

### 1.3 错误密码登录被拒

```bash
curl -s -X POST http://8.141.112.182/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13900001111","password":"WrongPass1!"}'
```

- [ ] 返回 "手机号或密码错误"

### 1.4 旧明文密码用户登录自动升级

使用演示账号（密码明文存储在旧数据库中）：

```bash
curl -s -X POST http://8.141.112.182/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800000001","password":"Abc123!"}'
```

**验证**: 登录成功后查 DB，`user` 的 password 应变为 `$2$` 开头。

- [ ] 登录成功
- [ ] 数据库密码已被自动升级为 BCrypt 哈希

---

## 二、JWT 令牌认证

### 2.1 登录返回 JWT token

```bash
curl -s -X POST http://8.141.112.182/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800000002","password":"Abc123!"}'
```

- [ ] data.token 存在且为较长的 Base64 字符串（JWT 格式）

### 2.2 Bearer token 访问需认证接口

```bash
TOKEN="<上一步获取的 token>"
curl -s http://8.141.112.182/api/cart/list/13800000002 \
  -H "Authorization: Bearer $TOKEN"
```

- [ ] 返回 code=200，正常返回数据

### 2.3 无效 token 被拒绝

```bash
curl -s http://8.141.112.182/api/cart/list/13800000002 \
  -H "Authorization: Bearer invalidtoken123"
```

- [ ] 返回 code=401，"登录已过期，请重新登录"

### 2.4 向后兼容 phone 直传（过渡期）

```bash
curl -s http://8.141.112.182/api/cart/list/13800000002 \
  -H "Authorization: 13800000002"
```

- [ ] 返回 code=200，正常工作

### 2.5 注册成功直接返回 token

```bash
curl -s -X POST http://8.141.112.182/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"13900002222","username":"newuser","password":"Abc123!","confirmPassword":"Abc123!","code":"000000"}'
```

- [ ] 返回 data.token 字段

---

## 三、HTTP 状态码规范化

### 3.1 未登录 → 401

```bash
curl -s -o /dev/null -w "%{http_code}" http://8.141.112.182/api/cart/list/13800000001
```

- [ ] HTTP 状态码为 401

### 3.2 权限不足 → 403

```bash
TOKEN="<user 角色的 token>"
curl -s -o /dev/null -w "%{http_code}" http://8.141.112.182/api/admin/dashboard \
  -H "Authorization: Bearer $TOKEN"
```

- [ ] HTTP 状态码为 403

### 3.3 业务错误 → 400

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST http://8.141.112.182/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800000001","password":"wrong"}'
```

- [ ] HTTP 状态码为 400

### 3.4 成功请求 → 200

```bash
curl -s -o /dev/null -w "%{http_code}" http://8.141.112.182/api/merchant/list
```

- [ ] HTTP 状态码为 200

---

## 四、CORS 安全收紧

### 4.1 浏览器从允许源访问

在 `http://localhost:5173` 页面中通过浏览器控制台执行：

```js
fetch('http://8.141.112.182/api/merchant/list').then(r => r.json()).then(console.log)
```

- [ ] 请求成功，返回商家列表

### 4.2 curl 模拟非允许源（无 Origin 头不受限）

```bash
curl -s -H "Origin: http://evil.com" http://8.141.112.182/api/merchant/list -v 2>&1 | grep -i "access-control"
```

- [ ] 响应不含 `Access-Control-Allow-Origin` 头（或值为配置的白名单域名，非 `*`）

---

## 五、前端适配

### 5.1 登录后 token 存储

1. 打开 http://8.141.112.182/login
2. 用演示账号登录（user / Abc123!）
3. 打开浏览器 DevTools → Application → Local Storage

- [ ] `clas_user` 中包含 `token` 字段

### 5.2 API 请求携带 Bearer token

1. 登录后进入购物车页面 `/cart`
2. DevTools → Network 查看任意 `/api/*` 请求

- [ ] 请求头 `Authorization` 为 `Bearer <token>` 格式

### 5.3 退出登录清除 token

1. 点击页面"退出"
2. 检查 Local Storage

- [ ] `clas_user` 已清除
- [ ] 后续请求不再携带 Authorization 头

---

## 六、GitHub Actions 自动部署

### 6.1 推送触发自动部署

```bash
git push upstream dev
```

然后访问 https://github.com/Amyjinru/CLAS/actions

- [ ] 出现新的 "Deploy to Cloud Server" 运行
- [ ] 运行结果为 ✅ 通过

### 6.2 手动触发部署

访问 https://github.com/Amyjinru/CLAS/actions/workflows/deploy.yml → Run workflow

- [ ] 手动触发成功执行

### 6.3 部署后服务正常

```bash
curl -s http://8.141.112.182/api/health
```

- [ ] 返回 `{"code":200,...}`

---

## 七、配置文件安全

### 7.1 缺少密码环境变量时启动失败

在服务器上临时取消 `MYSQL_PASSWORD` 环境变量后重启：

```bash
# 预期：Spring Boot 启动失败，提示缺少 datasource password 配置
```

- [ ] 无 `MYSQL_PASSWORD` 时启动失败（验证配置生效）

---

## 八、测试汇总

| 模块 | 测试项 | 通过 |
|------|--------|------|
| 密码加密 | 注册存 BCrypt / 登录验证 / 错误拒绝 / 旧密码升级 | [ ] |
| JWT | 登录返回 / Bearer 访问 / 无效拒绝 / 兼容旧模式 / 注册返回 | [ ] |
| 状态码 | 401 / 403 / 400 / 200 | [ ] |
| CORS | 允许源访问 / 非允许源拒绝 | [ ] |
| 前端 | token 存储 / Bearer 携带 / 退出清除 | [ ] |
| 自动部署 | push 触发 / 手动触发 / 健康检查 | [ ] |
| 配置安全 | 缺密码启动失败 | [ ] |
| **合计** | **22 项** | [ ] |
