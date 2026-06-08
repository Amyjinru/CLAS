# CLAS MVP 极简框架

这是按软件详细设计说明书核心验收流程重建的最小可运行版本。第一阶段只保留外卖下单闭环：

```text
浏览商家 -> 浏览商品 -> 加入购物车 -> 提交订单 -> 模拟支付 -> 商家接单 -> 确认完成 -> 评价
```

当前版本已接入 Redis 验证码、模拟支付、公告管理、数据统计与管理员后台；鉴权仍采用轻量级 `Authorization: <phone>` 演示方案，尚未接入 JWT / Spring Security。收藏功能暂未实现。

> 第二阶段已实现商家入驻、5态状态机、管理员审核、RBAC 权限控制，详见下方「功能改进」章节。

## 技术栈

- 后端：Spring Boot 3、MyBatis Plus、MySQL、Redis、Lombok
- 前端：Vue3、Vite、axios
- 数据库：10 张业务表，见 `database/schema.sql`

## 项目结构

```text
backend/src/main/java/com/clas
├── common      # Result、业务异常、全局异常处理、商家状态枚举
├── config      # CORS、AuthInterceptor、RequireRole、UserContext、MyMetaObjectHandler
├── controller  # REST API（用户、商家、商品、购物车、订单、评价、健康检查）
├── dto         # 请求与响应 DTO（含商家入驻、审核）
├── entity      # 业务表实体（含商家审核日志、支付、公告）
├── mapper      # MyBatis Plus Mapper（含审核日志 Mapper）
└── service     # 用户、商家、商品、购物车、订单、评价
```

## 初始化数据库

```bash
mysql -h127.0.0.1 -P3306 -uroot < database/schema.sql
```

脚本会创建 `clas` 数据库、重建业务表并插入演示账号、商家、商品和公告数据。

## 启动后端

确认 `backend/src/main/resources/application.yml` 中的 MySQL 用户名和密码正确，并启动本地 Redis（默认 `127.0.0.1:6379`）后运行：

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

| 角色 | 手机号 | 展示名 | 密码 |
| --- | --- | --- | --- |
| 用户 | `13800000001` | `user` | `Abc123!` |
| 商家 | `13800000002` | `merchant` | `Abc123!` |
| 管理员 | `13800000003` | `admin` | `Abc123!` |

当前版本采用轻量级演示鉴权：前端把当前登录用户保存在 `localStorage`，请求时携带手机号作为 `Authorization` Header，后端按手机号加载用户并做角色校验。

## 同学 A 维护：用户模块与接口约定

### 用户接口

| 接口 | 说明 |
| --- | --- |
| `POST /api/user/register/send-code` | 发送注册验证码，验证码存 Redis，控制台模拟短信输出 |
| `POST /api/user/register` | 手机号注册，默认角色为 `USER` |
| `POST /api/user/login` | 手机号登录，成功后返回当前用户信息 |

注册规则：

- `phone` 必填且唯一，是用户表主键。
- `username` 必填，仅作为展示名，允许重复。
- `password` 必填，至少 6 位，必须包含大小写英文字母、数字和特殊符号。
- `code` 必填，必须匹配 Redis 中保存的验证码。
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
  "message": "手机号或密码错误",
  "data": null
}
```

### 简易鉴权与角色

- 当前阶段不接入 JWT / Spring Security，`Authorization: <phone>` 仅适合课程演示。
- 前端登录后将用户信息写入 `localStorage` 的 `clas_user`。
- 后端通过请求头 `Authorization: <phone>` 获取当前用户，购物车、订单、支付、评价等私有操作以服务端当前用户为准，不信任请求体中的 `userId`。
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
- 未登录游客：提供 accountPhone + 验证码 + username + password，系统自动创建商家账号并入驻
- 商家账号手机号 `accountPhone` 与店铺联系电话 `contactPhone` 可分开填写
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

- `AuthInterceptor`：解析 `Authorization` Header（手机号）→ 查 DB → 写入 `UserContext`（ThreadLocal）
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
---

## 功能改进：管理后台 + 数据统计 + 前端整合（同学E）

### 一、管理后台仪表盘

**API**

| 接口 | 说明 |
| --- | --- |
| `GET /api/admin/dashboard` | 仪表盘汇总（总用户/商家/订单/销售额 + 今日数据） |
| `GET /api/admin/stats/orders` | 订单统计（按状态分布 + 近7天每日趋势） |
| `GET /api/admin/stats/sales` | 销售额统计（按日期趋势 + 总/月/周销售额） |
| `GET /api/admin/stats/merchants` | 商家排行（按销售额 + 按评分，Top 10） |
| `GET /api/admin/stats/products` | 热销商品排行（按销量，Top 10） |

**前端**

- `/admin/dashboard` 仪表盘页面：4个统计卡片 + 今日概览 + 订单状态饼图 + 近7天销售额柱线混合图 + 商家排行 + 热销商品

### 二、管理员统一管理后台

**API**（均需 `@RequireRole("ADMIN")` 权限）

| 接口 | 说明 |
| --- | --- |
| `GET /api/admin/users` | 用户列表（分页，屏蔽密码字段） |
| `PUT /api/admin/users/{id}/status` | 禁用/启用用户账号 |
| `GET /api/admin/orders` | 全平台订单列表（分页 + 状态筛选） |
| `GET /api/admin/reviews` | 全平台评价列表（含关联用户/商家信息） |
| `DELETE /api/admin/reviews/{id}` | 删除评价并自动重新计算商家评分 |

**前端**（AdminLayout 侧边栏布局）

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/admin/dashboard` | 仪表盘 | ECharts 统计图表 |
| `/admin/orders` | 订单管理 | 全平台订单分页列表 |
| `/admin/users` | 用户管理 | 用户列表 + 禁用/启用 |
| `/admin/audit` | 商家审核 | 整合自第二阶段 |
| `/admin/reviews` | 评价管理 | 评价列表 + 删除 |
| `/admin/announcements` | 公告管理 | 整合自第四阶段 |

### 三、安全修复

- `AnnouncementController` 的创建/修改/删除操作添加 `@RequireRole("ADMIN")` 权限保护
- `UserService.login()` 增加 `enabled` 字段校验，禁用用户无法登录
- 用户表新增 `enabled TINYINT(1) NOT NULL DEFAULT 1` 字段

### 四、UI 全面优化 —「暖食」设计语言

- 暖琥珀主色调（#f97316）+ 深青强调色（#0d9488）
- 完整 CSS 变量体系覆盖 Element Plus 所有组件
- 管理后台深咖啡色侧边栏固定定位，切换页面不抖动
- 毛玻璃顶栏 + 卡片圆角阴影 + 按钮悬浮微动效
- ECharts 图表配色与主题统一
- 导航栏按角色（USER/MERCHANT/ADMIN）分离显示

### 五、数据库变更

| 变更 | 说明 |
| --- | --- |
| `user` 表新增字段 | `enabled TINYINT(1) NOT NULL DEFAULT 1`，支持管理员禁用用户 |

### 六、新增文件

| 文件 | 说明 |
| --- | --- |
| `controller/AdminController.java` | 管理后台统一控制器 |
| `service/StatisticsService.java` | 数据聚合统计服务 |
| `dto/DashboardStats.java` 等 5 个 DTO | 仪表盘/统计/排行数据对象 |
| `views/admin/AdminLayout.vue` | 管理后台侧边栏布局 |
| `views/admin/AdminDashboardView.vue` | ECharts 仪表盘页面 |
| `views/admin/AdminUsersView.vue` | 用户管理页面 |
| `views/admin/AdminOrdersView.vue` | 订单管理页面 |
| `views/admin/AdminReviewsView.vue` | 评价管理页面 |
| `styles/theme.css` | CSS 主题变量体系 |
# 2026-06-08 开发进度快照

当前 `dev` 分支已经从外卖下单 MVP 扩展为“生活助手平台”综合演示版本，围绕作业第二个开发建议补齐了用户、商家、管理员三端能力：

- 用户端：商家搜索/筛选、商品下单、地址管理、团购券购买、收藏店铺、消息通知、订单退款申请、生活服务预约、评价与举报。
- 商家端：接单/配送/完成、团购券创建与核销、评价回复、退款审核、预约确认/完成/取消、公告查看。
- 管理端：商家审核、仪表盘统计、用户/订单/评价/公告管理、评价举报处理。
- 数据库：当前 schema 已扩展到 16 张业务表，新增/调整内容见 `database/schema.sql`，协作同学拉取后需要重新执行脚本或同步迁移表结构。
- 鉴权说明：仍采用课程演示级 `Authorization: <phone>` + `@RequireRole` 方案，尚未接入生产级 JWT / Spring Security。

新增页面入口：

| 角色 | 路由 | 功能 |
| --- | --- | --- |
| USER | `/home` | 商家搜索、筛选、推荐排序 |
| USER | `/deals` | 团购券列表与购买 |
| USER | `/bookings` | 生活服务预约提交与取消 |
| USER | `/profile` | 地址、收藏、通知中心 |
| MERCHANT | `/merchant-console` | 订单、配送、退款、评价回复、核销 |
| MERCHANT | `/merchant/deals` | 团购券管理 |
| MERCHANT | `/merchant/bookings` | 预约管理 |
| ADMIN | `/admin/reviews` | 评价举报处理 |
