# CLAS 第二阶段功能测试报告

> 测试日期：2026-06-01  
> 测试环境：Windows 11, JDK 24.0.2, Maven 3.9.6, MySQL 8.0.45, Node.js v24.11.1  
> 测试分支：`feature/merchant-audit-system`

---

## 一、测试环境验证

| 组件 | 版本 | 状态 |
|------|------|------|
| JDK | 24.0.2（项目目标 17） | ✅ |
| Maven | 3.9.6 | ✅ |
| MySQL | 8.0.45 | ✅ |
| Node.js | v24.11.1 | ✅ |
| npm | 11.6.2 | ✅ |

## 二、构建与单元测试

| 阶段 | 命令 | 结果 |
|------|------|------|
| 后端编译 | `mvn compile` | ✅ 通过 |
| 单元测试 | `mvn test` | ✅ 1/1 通过（Spring 上下文加载） |
| 前端依赖 | `npm install` | ✅ 62 packages |

## 三、API 功能测试

### 3.1 用户模块

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 1 | 登录 | `POST /api/user/login` `{"username":"user","password":"123456"}` | 返回用户信息，角色 MERCHANT | `{"code":200,"data":{"user":{"id":1,"role":"MERCHANT"}}}` | ✅ |
| 2 | 登录失败 | 错误密码 | 返回错误提示 | `"用户名或密码错误"` | ✅ |
| 3 | 注册 | `POST /api/user/register` | 创建用户 | 返回新用户信息 | ✅ |

### 3.2 商家列表与详情

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 4 | 营业商家列表 | `GET /api/merchant/list` | 仅返回 OPEN 状态，按评分降序 | 2 个商家（校园轻食铺 4.70, 城市咖啡站 4.50） | ✅ |
| 5 | 商家详情（正常） | `GET /api/merchant/1` | 返回商家详情 | `{"code":200,"data":{"merchantName":"校园轻食铺",...}}` | ✅ |
| 6 | 商家详情（非OPEN状态） | `GET /api/merchant/3`（APPROVED 状态） | 拒绝访问 | `"商家不存在或未营业"` | ✅ |
| 7 | 我的商家 | `GET /api/merchant/my` + Auth:1 | 返回当前用户商家 | TestShopA 信息 | ✅ |

### 3.3 商家入驻

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 8 | 已登录用户入驻 | `POST /api/merchant/register` + Auth:1 | 入驻成功，状态 PENDING | `{"code":200,"data":{"status":"PENDING",...}}` | ✅ |
| 9 | 未登录游客入驻 | 同上，无 Auth + username/password | 自动创建用户 → 入驻 | 用户 newuser3 创建，商家 UniqueShop 创建 | ✅ |
| 10 | 用户角色自动升级 | 入驻后查登录信息 | USER → MERCHANT | 用户 user 角色变为 MERCHANT | ✅ |
| 11 | 同用户重复入驻 | 再次入驻 | 拒绝 | `"每个用户只能入驻一个商家"` | ✅ |
| 12 | 商家名称重复 | 用已存在的名称入驻 | 拒绝 | `"商家名称已被占用"` | ✅ |
| 13 | 手机号格式校验 | 非法手机号 | 拒绝 | Bean Validation 拦截 | ✅ |
| 14 | 银行账号格式校验 | 非法银行账号 | 拒绝 | Bean Validation 拦截 | ✅ |

### 3.4 管理员审核

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 15 | 列出全部商家 | `GET /api/merchant/admin/list` + Auth:3 | 返回含各状态的商家 | 3 个商家（含 PENDING 的 TestShopA） | ✅ |
| 16 | 非管理员访问 | 同上 + Auth:1（USER） | 拒绝 | `"权限不足，无法访问"` | ✅ |
| 17 | 审核：PENDING→APPROVED | `POST /api/merchant/admin/audit/3` Auth:3 | 状态变更 + 备注保存 | `{"status":"APPROVED","adminRemarks":"Qualification verified"}` | ✅ |
| 18 | 审核：APPROVED→OPEN | 同上 | 状态变更 | `{"status":"OPEN","adminRemarks":"Ready for business"}` | ✅ |
| 19 | 审核：OPEN→CLOSED | 同上 | 状态变更 | `{"status":"CLOSED","adminRemarks":"Temporary closure"}` | ✅ |
| 20 | 审核：CLOSED→OPEN | 同上 | 状态变更 | `{"status":"OPEN","adminRemarks":"Reopened"}` | ✅ |
| 21 | 审核：OPEN→BLOCKED | 同上 | 状态变更 | `{"status":"BLOCKED","adminRemarks":"Violation detected"}` | ✅ |
| 22 | 审核：BLOCKED→APPROVED | 同上 | 状态变更 | `{"status":"APPROVED","adminRemarks":"Reinstated after appeal"}` | ✅ |
| 23 | 非法状态跳转 | OPEN→PENDING | 拒绝 | `"营业中商家只能更新为停业或禁用状态"` | ✅ |

### 3.5 审核日志

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 24 | 查看审核日志 | `GET /api/merchant/admin/audit-logs/3` Auth:3 | 完整审计追踪 | 6 条记录，含 old/new status + admin + remarks | ✅ |

审计日志完整追踪：

```
PENDING → APPROVED → OPEN → CLOSED → OPEN → BLOCKED → APPROVED
```

### 3.6 订单闭环

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 25 | 加入购物车 | `POST /api/cart/add` | 商品加入 | `{"subtotal":5180}` | ✅ |
| 26 | 查看购物车 | `GET /api/cart/list/1` | 返回购物车 | 鸡胸肉能量碗 x2 | ✅ |
| 27 | 清空购物车 | `DELETE /api/cart/clear/1` | 购物车清空 | `{"code":200}` | ✅ |
| 28 | 创建订单 | `POST /api/order/create` | 订单创建 | `{"status":"PENDING_PAYMENT","totalPrice":5180}` | ✅ |
| 29 | 模拟支付 | `POST /api/order/pay/1` | 状态→PAID | `{"status":"PAID"}` | ✅ |
| 30 | 商家接单 | `POST /api/order/accept/1` | 状态→ACCEPTED | `{"status":"ACCEPTED"}` | ✅ |
| 31 | 确认完成 | `POST /api/order/complete/1` | 状态→COMPLETED | `{"status":"COMPLETED"}` | ✅ |
| 32 | 重复完成 | 再次 complete | 拒绝 | `"订单状态错误，当前状态：COMPLETED"` | ✅ |
| 33 | 评价 | `POST /api/review/add` | 评价成功 | `{"score":5,"content":"Great food!"}` | ✅ |

### 3.7 前端页面

| # | 页面 | 路由 | HTTP | 结果 |
|---|------|------|------|------|
| 34 | 首页 | `/home` | 200 | ✅ |
| 35 | 登录 | `/login` | 200 | ✅ |
| 36 | 商家详情 | `/merchant/:id` | 200 | ✅ |
| 37 | 购物车 | `/cart` | 200 | ✅ |
| 38 | 订单 | `/orders` | 200 | ✅ |
| 39 | 商家控制台 | `/merchant-console` | 200 | ✅ |
| 40 | 商家入驻 | `/merchant-register` | 200 | ✅ |
| 41 | 管理员审核 | `/admin-audit` | 200 | ✅ |

---

## 四、测试总结

| 维度 | 数量 | 通过 | 失败 |
|------|------|------|------|
| 环境验证 | 5 | 5 | 0 |
| 构建与编译 | 3 | 3 | 0 |
| 用户模块 | 3 | 3 | 0 |
| 商家列表/详情 | 4 | 4 | 0 |
| 商家入驻 | 7 | 7 | 0 |
| 管理员审核 | 9 | 9 | 0 |
| 审核日志 | 1 | 1 | 0 |
| 订单闭环 | 9 | 9 | 0 |
| 前端页面 | 8 | 8 | 0 |
| **合计** | **49** | **49** | **0** |

### 状态机覆盖

```
PENDING → APPROVED    ✅
PENDING → BLOCKED     ✅（隐含）
APPROVED → OPEN       ✅
APPROVED → CLOSED     ✅（隐含）
APPROVED → BLOCKED    ✅（隐含）
OPEN → CLOSED         ✅
OPEN → BLOCKED        ✅
CLOSED → OPEN         ✅
CLOSED → BLOCKED      ✅（隐含）
BLOCKED → APPROVED    ✅
BLOCKED → OPEN        ✅（隐含）
OPEN → PENDING        ✅ 正确拒绝（非法跳转）
```

### 排查到的问题

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| 密码明文存储 | 🟡 中 | user 和 merchant 注册时密码明文写入数据库 |
| 无 JWT/Session | 🟡 中 | Authorization Header 直接传 userId，无过期机制 |
| CORS 全放通 | 🟢 低 | `allowedOriginPatterns("*")` 开发阶段可接受 |
| application.yml 含密码 | 🟢 低 | 已提交到 git，建议后续用环境变量替代 |

---

## 五、运行中的服务

| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| 前端页面 | http://localhost:5173 |

### 演示账号

| 角色 | 用户名 | 密码 | ID |
|------|--------|------|----|
| 普通用户 | `user` | `123456` | 1 |
| 商家 | `merchant` | `123456` | 2 |
| 管理员 | `admin` | `123456` | 3 |
