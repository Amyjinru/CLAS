## Context

CLAS 当前已经有三端角色和多个业务模块。下一阶段的主要风险不是“任务不够”，而是多人同时修改同一批核心文件导致 merge 冲突和回归。因此任务池按功能边界、文件边界和依赖关系来拆分。

五人建议分工原则：

| 成员 | 主要职责 | 主要前端文件边界 | 主要后端文件边界 | 冲突控制 |
| --- | --- | --- | --- | --- |
| A 用户端体验 | 用户发现、购物车、订单详情、地址、通知 | `HomeView`, `MerchantDetailView`, `CartView`, `OrdersView`, `ProfileView`, `user/*` | 少量 `Address/Notification/Favorite` | 尽量不改商家端和后台 |
| B 商家端运营 | 商品、团购、预约、履约、商家入驻 | `MerchantConsoleView`, `MerchantProductsView`, `MerchantDealsView`, `MerchantBookingsView`, `MerchantRegisterView` | `Merchant/Product/Deal/Booking` | 避免改用户端订单页 |
| C 管理后台 | 审核、用户、订单、评价、公告、看板 | `admin/*`, `AdminAuditView` | `AdminController`, `StatisticsService`, `AnnouncementService` | 避免改业务核心 Service |
| D 交易后端 | 购物车、订单、支付、退款、优惠券/营销 | 新增或少量 API 对接 | `Cart/Order/Payment/Deal/Coupon` | 与 A 约定接口后并行 |
| E 数据库测试文档 | schema/migration/H2/test/docs/接口文档 | 少量测试辅助页面或不改前端 | entity/mapper/test/schema/docs | 数据库变更统一由 E 合并 |

协作原则：

- 每个功能任务尽量由一个主责成员完成，其他成员只消费接口。
- 所有数据库结构变更先由 E 统一排期，避免多个人同时改 `schema.sql`。
- 路由表、全局样式、axios client、`Result`、`AuthInterceptor` 这类高冲突文件需要提前约定窗口。
- 前后端并行时，先约定 DTO/API 字段，前端可用 mock 数据开发。

## Person-Based Assignment

相比按“前端/后端/数据库”横切分工，更推荐按人领取完整功能包。每个人负责一个相对独立的业务域，尽量自己完成该域的前端、后端、数据库和测试记录；数据库脚本最终仍由 E 统一合并。

### 成员 A：用户发现与个人中心包

| 功能名称 | 工作量 | 优先级 | 前端任务 | 后端任务 | 数据库任务 |
| --- | --- | --- | --- | --- | --- |
| 首页搜索筛选增强 | M | P0/P1 | 修改 `HomeView`：分类、关键词、排序、空结果、加载态 | 完善 `MerchantController` 列表查询参数；支持 keyword/category/sort | 可选增加商家 category 索引 |
| 商家详情体验完善 | M | P0 | 修改 `MerchantDetailView`：营业状态、配送费、起送价、商品售罄提示、收藏入口 | 完善商家详情响应；补充商品状态和库存提示 | 无 |
| 个人中心增强 | M | P0/P1 | 修改 `ProfileView`：地址编辑、收藏列表、通知中心、全部已读 | 完善 `Address/Favorite/Notification` 接口；批量已读 | `notification` 可加 `type`；地址默认唯一性由 E 合并 |

边界：A 主要碰用户端页面和地址/收藏/通知接口，不碰订单创建核心逻辑、不碰商家后台、不碰管理后台。

### 成员 B：商家运营包

| 功能名称 | 工作量 | 优先级 | 前端任务 | 后端任务 | 数据库任务 |
| --- | --- | --- | --- | --- | --- |
| 商品管理完善 | M | P0/P1 | 修改 `MerchantProductsView`：分类、上下架、售罄、图片兜底 | 完善 `ProductController/ProductService` 管理接口 | 可选 `product.category` 或 `product_category` |
| 商家履约工作台 | M | P0 | 修改 `MerchantConsoleView`：接单、拒单、配送中、完成、退款审核区块 | 完善商家订单状态操作和归属校验 | 可选 `orders.reject_reason` |
| 团购与预约运营 | L | P1 | 修改 `MerchantDealsView`、`MerchantBookingsView`：核销记录、过期状态、预约状态筛选 | 完善 `DealService/BookingService` 状态校验和核销记录 | 可选 `deal_redeem_log` |

边界：B 主要碰商家端页面和商品/团购/预约/商家订单处理，不碰用户订单页、不碰管理后台。

### 成员 C：管理后台与平台治理包

| 功能名称 | 工作量 | 优先级 | 前端任务 | 后端任务 | 数据库任务 |
| --- | --- | --- | --- | --- | --- |
| 商家审核详情增强 | M | P0 | 修改 `AdminAuditView`：筛选、详情、审核记录、备注展示 | 完善审核日志查询和审核备注返回 | 无或补审核字段 |
| 管理后台数据看板增强 | M | P1/P2 | 修改 `AdminDashboardView`：日期筛选、图表空状态、大屏模式可选 | 完善 `StatisticsService/AdminController` 统计接口 | 可选增加查询索引，由 E 合并 |
| 管理员运营治理 | L | P1 | 修改 `AdminUsers/Orders/Reviews/Announcements`：筛选、导出、举报处理、公告置顶 | 增加导出接口、评价举报状态、公告置顶/有效期 | 可选 `announcement.pinned/expires_at` |

边界：C 只做管理后台和平台治理，不改用户端、商家端业务页面；统计接口以只读为主，降低和 D 的交易逻辑冲突。

### 成员 D：交易闭环包

| 功能名称 | 工作量 | 优先级 | 前端任务 | 后端任务 | 数据库任务 |
| --- | --- | --- | --- | --- | --- |
| 购物车与下单规则 | L | P0 | 修改 `CartView`：金额明细、配送费、起送价、库存不足、订单备注 | 完善 `CartService/OrderService`：库存、单商家、起送价、配送范围、事务 | 可选 `orders.remark`、购物车唯一索引 |
| 支付与订单状态 | M | P0 | 修改 `PaymentView`、`OrdersView` 中支付状态展示 | 完善 `PaymentService/OrderService`：幂等、失败模拟、状态机 | 可选 `payment.order_id` 唯一约束 |
| 退款售后增强 | M | P1 | 修改 `OrdersView`：退款进度、原因分类、商家审核结果 | 完善退款申请/审核/通知联动 | 可选退款日志或退款时间字段 |

边界：D 负责交易写路径和状态机；尽量不改商家工作台 UI，只提供接口给 B；不改后台统计展示。

### 成员 E：工程支撑与亮点包

| 功能名称 | 工作量 | 优先级 | 前端任务 | 后端任务 | 数据库任务 |
| --- | --- | --- | --- | --- | --- |
| 数据库与迁移统一 | M | P0 | 无 | 检查 entity/mapper 与 schema 一致 | 统一 `schema.sql`、migration、H2 schema、索引、seed data |
| 测试报告与接口文档 | M | P0/P1 | 无或补截图 | 整理核心 API 测试、补 Service 测试 | 准备测试数据 |
| 亮点二选一支撑 | L | P2 | 若选地图/大屏，协助截图和说明；若选安全，协助登录态测试 | 可选 Swagger/OpenAPI、自动化测试或 JWT/BCrypt 支撑 | 处理对应 schema 变更 |

边界：E 不承担复杂业务 UI，主要做所有人的底座合并、测试验证和课程交付材料；所有 schema 修改最终由 E 汇总，其他人只提交字段需求。

### 推荐领取方式

```text
第一轮只做 P0：
A：首页/商详/个人中心体验
B：商品管理/商家工作台
C：审核详情/后台基础体验
D：购物车/下单/支付/退款
E：数据库同步/测试报告/演示数据

第二轮再做 P1：
A：搜索发现
B：团购预约增强
C：运营治理/导出
D：售后和优惠券
E：迁移/接口文档/自动化测试
```

这样分工的冲突会更少：

- A 和 B 基本不碰同一批 Vue 页面。
- C 独立在 `admin/*`。
- D 主要碰交易 Service 和少量用户端交易页。
- E 统一处理 schema 和文档，不和业务 UI 抢文件。
- 真正可能冲突的只有 `OrderService`、`schema.sql`、`router/index.js`，需要提前锁定。

## Goals / Non-Goals

**Goals:**

- Generate a concrete development task pool from the CLAS product roadmap.
- Assign tasks to five team members with low-overlap ownership.
- Include function name, workload, priority, frontend task, backend task, and database task for each item.
- Mark dependencies and merge-risk notes for planning.

**Non-Goals:**

- Implement the tasks inside this proposal.
- Lock the team into exact person names.
- Solve every P2 highlight in one sprint.
- Guarantee zero conflicts; the goal is to reduce likely conflicts.

## Decisions

### Decision 1: Split by user-facing domain first

Frontend merge conflicts are common when multiple people touch the same view files. The plan gives user-side pages to A, merchant-side pages to B, and admin-side pages to C.

### Decision 2: Centralize database ownership

Schema files are global and high risk. E owns schema/migration/test schema changes, while feature owners provide field requirements.

### Decision 3: Give transaction logic to one backend owner

Order, payment, refund, coupon, and stock updates are tightly coupled. D owns these backend flows to avoid inconsistent state transitions.

### Decision 4: Keep P2 highlights optional and isolated

P2 tasks should be selected after P0/P1 stability. The task pool includes them, but they should not all be developed simultaneously.

## Task Pool

工作量标记：

- S: 0.5-1 天
- M: 1-2 天
- L: 3-5 天
- XL: 1 周以上或跨多人

### P0 核心功能任务池

| 功能名称 | 主责 | 工作量 | 优先级 | 前端任务 | 后端任务 | 数据库任务 |
| --- | --- | --- | --- | --- | --- | --- |
| 账号与权限口径统一 | E + C | M | P0 | 检查登录跳转、权限不足提示、演示账号展示 | 区分未登录/无权限/禁用账号响应；统一错误信息 | 无结构变更；同步 seed 账号和文档 |
| 用户端空/错/加载态补齐 | A | M | P0 | `Home/Cart/Orders/Profile/Deals/Bookings` 增加空状态、loading、错误提示 | 仅补必要错误消息 | 无 |
| 购物车有效性校验 | A + D | M | P0 | 失效商品、库存不足、单商家限制提示；批量删除体验 | 校验库存、商品状态、商家一致性；清理失效项接口 | 可选增加 `cart(user_id, product_id)` 唯一索引 |
| 下单规则完善 | D + A | L | P0 | 下单页展示配送费、起送价、订单备注、金额明细 | 创建订单校验起送价、配送范围、库存、事务一致性 | `orders` 可加 `remark`；必要索引 |
| 支付幂等与失败模拟 | D | M | P0 | `PaymentView` 展示支付中、失败、已支付状态 | 防重复支付；模拟失败；支付状态查询语义清晰 | `payment` 可加唯一 `order_id` 或支付流水约束 |
| 商家履约状态可视化 | B + D | M | P0 | 商家订单卡片显示状态流转、拒单/配送中按钮 | 订单状态机校验；拒单理由；配送状态推进 | `orders` 可加 `reject_reason` |
| 订单详情与状态时间线 | A + D | M | P0 | 统一订单详情组件；用户看到状态时间线和售后入口 | 订单响应补充状态、明细、支付、退款信息 | 可选新增订单状态日志表，若时间不足则不加 |
| 商家入驻审核闭环 | B + C | L | P0 | 入驻进度页、审核失败原因、管理员审核详情 | 审核状态通知；审核备注返回；资料校验 | 可选资质字段或资质图片字段 |
| 通知中心完善 | A + C | M | P0 | 未读数、全部已读、通知分类、公告入口 | 通知分类、批量已读接口 | `notification` 加 `type`、索引 |
| P0 回归测试与演示脚本 | E | M | P0 | 记录关键页面截图 | 补核心流程测试或 API 验证记录 | 确认 seed data 支持完整演示 |

### P1 常见商业功能任务池

| 功能名称 | 主责 | 工作量 | 优先级 | 前端任务 | 后端任务 | 数据库任务 |
| --- | --- | --- | --- | --- | --- | --- |
| 优惠券/满减模块 MVP | D + A | L | P1 | 用户领券列表；下单选择优惠券；金额抵扣展示 | 领券、可用券查询、下单抵扣、使用状态更新 | 新增 `coupon`, `user_coupon` |
| 商品分类 MVP | B + E | M | P1 | 商家商品管理增加分类；商家详情按分类展示 | 商品分类 CRUD；商品绑定分类 | 新增 `product_category` 或 `product.category` |
| 团购券增强 | B + D | M | P1 | 券详情、过期/已用状态、核销记录展示 | 过期判断、核销记录、团购退款规则 | 可新增 `deal_redeem_log` |
| 搜索发现增强 | A + D | M | P1 | 首页综合搜索、筛选、历史搜索、排序控件 | 商家+商品搜索接口；销量/距离/价格排序 | 可新增搜索历史表；基础版可 localStorage |
| 配送规则增强 | D + A | M | P1 | 展示配送范围、配送费计算、预计送达 | 按距离计算配送费；超范围禁止下单 | 无或增加规则配置表 |
| 商家营业时间管理 | B + D | M | P1 | 商家设置营业时间/开关店；用户端展示休息中 | 营业时间校验；非营业时禁止下单 | 可复用 `business_hours/status` |
| 售后退款增强 | D + A + B | L | P1 | 用户退款进度；商家审核理由；订单页状态 | 退款原因分类、审核状态机、通知联动 | 可加退款日志表或退款时间字段 |
| 商家经营分析 | B + C | M | P1 | 商家端销售额、订单趋势、热销商品 | 商家维度统计接口 | 无，依赖现有订单数据 |
| 平台运营配置 | C + E | L | P1 | 管理端类目/费率/公告置顶配置页 | 配置 CRUD；业务读取配置 | 新增 `platform_config`, 可选 `merchant_category` |
| 客服/工单 MVP | A + C + D | L | P1 | 用户提交工单；管理员处理列表 | 工单创建、回复、关闭、通知 | 新增 `support_ticket`, `support_reply` |
| 数据导出 | C | S | P1 | 管理端导出按钮 | CSV 导出订单/用户/商家/评价 | 无 |
| 移动端适配验收 | A | M | P1 | 用户端核心页面 390px/768px/桌面适配 | 无 | 无 |

### P2 项目亮点任务池

| 功能名称 | 主责 | 工作量 | 优先级 | 前端任务 | 后端任务 | 数据库任务 |
| --- | --- | --- | --- | --- | --- | --- |
| 地图路线配送亮点 | A + D | L | P2 | 商家详情/下单页展示定位、距离、路线、预计时间和降级提示 | 封装距离/路线估算接口；无 Key 时 fallback | 无，复用经纬度字段 |
| 运营大屏模式 | C | M | P2 | 管理端大屏视图：销售、订单、商家排行、趋势图 | 聚合统计接口优化 | 无，必要时加索引 |
| JWT + BCrypt 安全升级 | D + E | XL | P2 | 登录态过期处理；退出登录；前端保存 token | Spring Security/JWT/BCrypt；拦截器替换 | 密码重新加密策略；seed 密码处理 |
| 自动化接口测试 | E + D | M | P2 | 无 | 核心流程集成测试/API 集合 | 测试库 seed 数据 |
| 智能推荐 MVP | D + A | M | P2 | 首页推荐区、推荐理由 | 基于收藏/订单/类别偏好的简单推荐接口 | 可新增 `user_behavior`，基础版可不加 |
| AI/FAQ 客服 | A + C | M | P2 | FAQ 问答页或客服入口 | 规则型问答接口；管理员维护 FAQ | 新增 `faq` 表 |
| 实时通知 | D + A + B | L | P2 | 用户/商家端实时通知角标 | SSE/WebSocket 推送订单、审核、退款事件 | 无或通知事件表 |
| OpenAPI/Swagger 文档 | E | S | P2 | 无 | 接入接口文档依赖或导出接口说明 | 无 |
| 文件上传 MVP | B + E | M | P2 | 商品图/资质图片上传控件 | 本地文件上传接口；静态资源访问 | 商品/商家表增加图片字段或资质表 |

## Parallel Sprint Recommendation

### Sprint 1: P0 稳定并行

| 成员 | 推荐任务 |
| --- | --- |
| A | 用户端空/错/加载态、购物车体验、订单详情时间线 |
| B | 商家履约状态可视化、商家入驻进度 |
| C | 管理员审核详情、权限提示、通知/公告管理体验 |
| D | 购物车校验、下单规则、支付幂等、订单状态机 |
| E | schema/test schema 同步、演示账号口径、P0 测试报告 |

### Sprint 2: P1 商业增强并行

| 成员 | 推荐任务 |
| --- | --- |
| A | 搜索发现增强、移动端适配 |
| B | 商品分类、团购券增强、商家营业管理 |
| C | 商家经营分析、平台运营配置、数据导出 |
| D | 优惠券模块、配送规则、售后退款增强 |
| E | 数据库迁移、索引、测试和文档同步 |

### Sprint 3: P2 亮点冲刺

建议只选一个组合：

- 组合 A：地图路线配送 + 运营大屏
- 组合 B：安全升级 + 自动化接口测试
- 组合 C：智能推荐 + FAQ 客服

## Merge Strategy

- 每人使用独立分支：`feature/a-user-ux`, `feature/b-merchant-ops`, `feature/c-admin-ops`, `feature/d-transaction-backend`, `feature/e-db-test-docs`。
- 每天先从 `dev` rebase/merge 一次再开发。
- 数据库变更由 E 合并到一个 migration 批次，其他人不要直接改 schema。
- 修改 `frontend/src/router/index.js`, `frontend/src/api/client.js`, `backend/common/Result.java`, `AuthInterceptor.java`, `database/schema.sql` 前先在群里锁文件。
- API DTO 先在文档或 issue 里约定字段，再前后端并行。
- 每个 PR 尽量控制在一个功能边界内，不混入样式全局重构。

## Risks / Trade-offs

- [Risk] D 负责交易后端任务较重 -> Mitigation: P0 先完成订单/支付，优惠券和退款增强放 Sprint 2。
- [Risk] E 成为数据库瓶颈 -> Mitigation: 每周固定两个 schema 合并窗口，小改动先用兼容字段。
- [Risk] A 和 D 同时改购物车/订单接口导致联调阻塞 -> Mitigation: 先冻结请求/响应 DTO，前端用 mock 数据。
- [Risk] C 修改后台统计时影响 D 的订单数据 -> Mitigation: C 只读统计，D 负责写路径和状态机。
- [Risk] P2 引入新技术导致回归 -> Mitigation: P2 独立分支，演示主路径稳定后再合并。

## Migration Plan

This change is planning-only.

When implementation starts:

1. Pick Sprint 1 P0 tasks first.
2. Create separate feature branches by owner.
3. E creates migration placeholders if database changes are needed.
4. D publishes API contracts for transaction-related changes.
5. Frontend owners develop against agreed contracts or mock data.
6. Merge backend/data foundations before UI final integration.
7. Run `mvn test`, frontend build, and manual smoke test before each sprint merge.
