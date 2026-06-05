# CLAS MVP 极简框架

这是按软件详细设计说明书核心验收流程重建的最小可运行版本。第一阶段只保留外卖下单闭环：

```text
浏览商家 -> 浏览商品 -> 加入购物车 -> 提交订单 -> 模拟支付 -> 商家接单 -> 确认完成 -> 评价
```

暂不接入 Redis、JWT、Spring Security、支付、公告管理、数据统计、收藏功能。

> 第二阶段已实现商家入驻、5态状态机、管理员审核、RBAC 权限控制，详见下方「功能改进」章节。

## 技术栈

- 后端：Spring Boot 3、MyBatis Plus、MySQL、Lombok
- 前端：Vue3、Vite、axios
- 数据库：8 张核心表，见 `database/schema.sql`

## 项目结构

```text
backend/src/main/java/com/clas
├── common      # Result、业务异常、全局异常处理、商家状态枚举
├── config      # CORS、AuthInterceptor、RequireRole、UserContext、MyMetaObjectHandler
├── controller  # REST API（用户、商家、商品、购物车、订单、评价、健康检查）
├── dto         # 请求与响应 DTO（含商家入驻、审核）
├── entity      # 8 张核心表实体（含商家审核日志）
├── mapper      # MyBatis Plus Mapper（含审核日志 Mapper）
└── service     # 用户、商家、商品、购物车、订单、评价
```

## 初始化数据库

```bash
mysql -h127.0.0.1 -P3306 -uroot < database/schema.sql
```

脚本会创建 `clas` 数据库、重建 7 张核心表并插入演示账号和商品数据。

## 启动后端

确认 `backend/src/main/resources/application.yml` 中的 MySQL 用户名和密码正确后运行：

```bash
cd backend
mvn spring-boot:run
```

默认端口：`http://localhost:8080`

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认端口：`http://localhost:5173`

## 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 用户 | `user` | `123456` |
| 商家 | `merchant` | `123456` |
| 管理员 | `admin` | `123456` |

第一版没有鉴权，前端只把当前登录用户保存在 `localStorage`。

## 同学 A 维护：用户模块与接口约定

### 用户接口

| 接口 | 说明 |
| --- | --- |
| `POST /api/user/register` | 用户注册，默认角色为 `USER` |
| `POST /api/user/login` | 用户登录，成功后返回当前用户信息 |

注册规则：

- `username` 必填且唯一。
- `password` 必填，第一版暂时明文存储。
- `phone` 可为空；填写时必须唯一。
- `role` 可为空；填写时只能是 `USER`、`MERCHANT`、`ADMIN`。
- 所有用户响应都会隐藏 `password` 字段。

### 统一返回格式

所有 `/api/**` 接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

业务错误统一返回 `code=400`，例如：

```json
{
  "code": 400,
  "message": "用户名或密码错误",
  "data": null
}
```

### 简易鉴权与角色

- 当前阶段不接入 JWT / Spring Security。
- 前端登录后将用户信息写入 `localStorage` 的 `clas_user`。
- 后端通过请求头 `Authorization: <userId>` 获取当前用户。
- 角色统一使用字符串：`USER`、`MERCHANT`、`ADMIN`。
- 需要权限的接口使用 `@RequireRole` 注解控制。

### 金额单位

所有金额字段统一使用 `INT`，单位为“分”；前端展示时再除以 `100` 转成“元”。

---

## 功能改进：商家列表/详情、商家入驻/状态

### 一、商家列表与详情

**API**

| 接口 | 说明 |
| --- | --- |
| `GET /api/merchant/list` | 营业中商家列表，按评分降序排列 |
| `GET /api/merchant/{id}` | 商家详情，仅 OPEN 状态可查看，非营业状态返回错误提示 |
| `GET /api/merchant/my` | 当前登录用户的商家信息 |

**前端**

- `/home` 首页展示营业中商家列表
- `/merchant/:id` 商家详情页展示商品列表，支持加入购物车

### 二、商家入驻系统

**API**

| 接口 | 说明 |
| --- | --- |
| `POST /api/merchant/register` | 商家入驻申请 |

**入驻逻辑**

- 已登录用户：自动关联当前账号，角色不足时自动升级 `USER → MERCHANT`
- 未登录游客：提供 username + password，系统自动创建账号并入驻
- 防重复入驻：一个用户只能入驻一个商家
- 唯一性校验：商家名称、联系电话不可重复
- 参数校验：手机号格式（`^1[3-9]\d{9}$`）、银行账号格式（`\d{9,25}`）

**前端**

- `/merchant-register` 商家入驻申请表单

### 三、商家 5 态状态机

**状态流转**

```text
PENDING（待审核）──→ APPROVED（已审核）──→ OPEN（营业中）──→ CLOSED（停业）
   │                     │                    │                  │
   └─────────────────→ BLOCKED（禁用）←─────────────────────────┘
```

- `PENDING`：入驻后默认状态，等待管理员审核
- `APPROVED`：审核通过，待开通营业
- `OPEN`：正常营业，可接单，在列表页展示
- `CLOSED`：商家主动停业
- `BLOCKED`：管理员禁用（违规等）

**状态跳转校验**：`MerchantStatusEnum` + `validateStatusTransition()` 严格校验合法跳转，非法操作精确拒绝并返回中文错误提示。

**前端商家控制台** `/merchant-console`

| 状态 | UI 表现 |
| --- | --- |
| PENDING | 黄色提示「正在审核中」 |
| APPROVED | 蓝色提示「已审核，等待开通营业」 |
| OPEN | 正常营业面板 + 待接单管理列表 |
| CLOSED | 灰色提示「停业中」+ 锁定工作台 |
| BLOCKED | 红色警告「已被禁用」+ 锁定工作台 |

### 四、管理员审核系统

**API**（均需 `@RequireRole("ADMIN")` 权限）

| 接口 | 说明 |
| --- | --- |
| `GET /api/merchant/admin/list` | 查看全部商家（含各状态） |
| `POST /api/merchant/admin/audit/{id}` | 审核商家，变更状态 + 备注 |
| `GET /api/merchant/admin/audit-logs/{id}` | 查看商家审核日志 |

**审计追踪**：每次审核操作写入 `merchant_audit_log` 表（操作人、旧状态、新状态、备注、时间）。

**前端**：`/admin-audit` 管理员审核面板（仅 ADMIN 角色可访问）。

### 五、RBAC 权限控制

- `AuthInterceptor`：解析 `Authorization` Header（userId）→ 查 DB → 写入 `UserContext`（ThreadLocal）
- `@RequireRole`：声明式角色校验注解，标注在 Controller 方法上即可拦截非授权访问
- 前端路由守卫：`meta.roles: ['ADMIN']` 拦截非管理员页面访问

### 六、数据库变更

| 变更 | 说明 |
| --- | --- |
| `merchant` 表新增字段 | `user_id`, `phone`(UNIQUE), `bank_account`, `admin_remarks`, `settlement_cycle`, `created_at`, `updated_at` |
| 新增 `merchant_audit_log` 表 | `id`, `merchant_id`, `admin_id`, `old_status`, `new_status`, `remarks`, `created_at` |
| `user.phone` | 改为 UNIQUE 约束 |

### 七、其他改进

- `DELETE /api/cart/clear/{userId}` 清空购物车
- `MyMetaObjectHandler` 自动填充 `created_at` / `updated_at` 时间戳
