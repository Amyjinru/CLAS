## Context

成员 C 的目标是把 CLAS 管理后台从“能进入、能列表操作”提升到“能演示平台治理闭环”。当前后台已有页面和接口，但存在以下现状：

- `AdminAuditView.vue` 已能列出商家、更新状态、查看日志，但缺少筛选、详情视图和更清晰的审核上下文。
- `AdminDashboardView.vue` 已展示基础统计、订单状态图、销售趋势、商家/商品排行，但没有日期范围筛选、空状态、大屏模式和统一刷新反馈。
- `AdminUsersView.vue` 已有用户分页、禁用/启用、处罚操作，但筛选能力不足。
- `AdminOrdersView.vue` 已有状态筛选和分页，但缺少日期/用户/商家等运营筛选。
- `AdminReviewsView.vue` 已有删评申请、举报处理和删除能力，但需要更清晰的处理状态、筛选和备注展示。
- `AdminAnnouncementsView.vue` 已能增删改公告，但缺少置顶/有效期等运营能力。
- 多个后台页面存在乱码文案，影响可读性和验收。

## Goals / Non-Goals

**Goals:**

- 修复管理后台页面中文文案乱码。
- 完成 P0 商家审核详情增强：筛选、详情、审核日志、备注回显、补审核提示。
- 完成 P1 管理员运营治理：用户、订单、评价、公告的筛选和处理状态增强。
- 完成 P1/P2 数据看板增强：日期筛选、空状态、刷新反馈、大屏模式可选。
- 后端接口以只读查询和轻量状态更新为主，保证权限边界仍由 `@RequireRole("ADMIN")` 控制。
- 实现后能通过前端构建、后端测试/打包和服务器部署健康检查。

**Non-Goals:**

- 不改用户端页面。
- 不改商家端业务页面，如商家控制台、商品管理、接单流程。
- 不改下单、支付、退款等交易核心逻辑。
- 不引入复杂权限系统或 JWT。
- 不重构全局设计系统。

## Decisions

### Decision 1: 先修可演示性，再补功能

后台页面存在乱码，影响所有 C 包功能的验收，因此优先统一修复 `AdminAuditView`、`AdminDashboardView`、`AdminUsersView`、`AdminOrdersView`、`AdminReviewsView`、`AdminAnnouncementsView` 中的中文文案和明显模板破损。

备选方案：只做新增功能。暂不采用，因为新增功能叠在乱码页面上会降低演示质量。

### Decision 2: 商家审核 P0 以“上下文充分”为核心

审核列表需要支持状态筛选和关键词筛选；详情视图需要展示商家基本资料、入驻时间、审核备注、当前状态、状态流转日志。审核操作需要强制管理员能填写备注或至少保留备注入口。

备选方案：只增加状态下拉筛选。暂不采用，因为无法满足“详情、审核记录、备注展示”的要求。

### Decision 3: 看板筛选保持只读，不影响交易逻辑

数据看板的日期筛选应通过统计接口参数完成，统计口径只读，不修改订单、支付或商家业务状态。前端支持默认近 7 天，后端支持 `startDate`、`endDate` 可选参数。

备选方案：前端拿全量数据后自行筛选。暂不采用，因为当前统计在后端聚合，保持后端统一口径更清晰。

### Decision 4: 运营治理优先筛选与处理闭环

用户、订单、评价、公告页面优先增加运营筛选、空状态、操作反馈和处理备注。导出能力和公告置顶/有效期为可选增强，若实现需要保持接口简单。

备选方案：一次性新增复杂审计中心。暂不采用，因为会扩大范围并和既有页面重复。

## Proposed Behavior

### 商家审核详情增强

后台路径：`/admin/audit`

前端能力：

- 状态筛选：全部、待审核、已审核、营业中、停业中、已禁用。
- 关键词筛选：商家名称、手机号、品类、地址。
- 详情抽屉/弹窗：
  - 商家名称、联系人、电话、地址、品类、营业时间、结算周期、银行卡尾号/账号字段按已有数据展示。
  - 当前状态、入驻时间、更新时间、管理员备注。
  - 状态流转日志时间线。
- 审核操作：
  - 更新状态前展示当前商家和状态。
  - 备注输入明确提示“说明审核原因或处理依据”。
  - 操作成功后刷新列表、详情和日志。
- 空状态：
  - 无待审核商家时显示“暂无待审核商家”。
  - 无日志时显示“暂无审核记录”。

后端能力：

- 复用或完善商家审核列表接口，确保返回 `adminRemarks`、`createdAt`、`updatedAt` 等字段。
- 复用或完善审核日志接口，按时间倒序返回状态变更、管理员、备注。
- 可选：增加审核状态统计接口，返回各状态商家数量用于筛选徽标。

### 管理后台数据看板增强

后台路径：`/admin/dashboard`

前端能力：

- 日期范围筛选，默认近 7 天。
- 刷新按钮和加载状态。
- 图表空状态：无订单/销售数据时不显示空白图。
- 大屏模式可选：隐藏侧栏或使用更适合展示的图表布局。
- 统计卡片保持紧凑展示：总用户、总商家、总订单、总销售额、今日订单、待处理项。

后端能力：

- `GET /api/admin/dashboard` 可选支持 `startDate`、`endDate`。
- `GET /api/admin/stats/orders` 可选支持 `startDate`、`endDate`。
- `GET /api/admin/stats/sales` 可选支持 `startDate`、`endDate`。
- 统计接口保持只读，不写数据库。

### 管理员运营治理

#### 用户管理

- 增加角色筛选、启用状态筛选、关键词搜索。
- 保留禁用/启用和处罚能力。
- 处罚弹窗展示处罚类型、原因、时长和确认提示。
- 可选导出当前筛选结果。

#### 订单管理

- 增加日期范围、用户/商家关键词、订单号筛选。
- 列表展示订单状态、金额、下单时间，必要时展示拒单原因/退款状态字段。
- 保持只读，不修改订单状态。
- 可选导出当前筛选结果。

#### 评价治理

- 增加举报/删评申请状态筛选。
- 展示处理备注、处理状态和发起来源。
- 处理举报或删评申请后刷新列表。
- 可选展示已删除评价备份入口。

#### 公告管理

- 修复表单和列表文案。
- 增加公告状态筛选。
- 可选支持置顶、有效期：
  - 若现有表字段不足，增加轻量迁移。
  - 前端列表展示置顶标识和有效期。

## Backend API Plan

### Required

- Keep existing admin auth:
  - `@RequireRole("ADMIN")`
- Merchant audit:
  - ensure list endpoint includes fields required by detail view.
  - ensure log endpoint returns ordered records with remarks.
- AdminController filters:
  - `/api/admin/users`: add optional `role`, `enabled`, `keyword`.
  - `/api/admin/orders`: add optional `status`, `startDate`, `endDate`, `keyword`.
  - `/api/admin/reviews`: add optional `reportStatus`, `keyword`.
- StatisticsService:
  - add optional date-range parameters where needed.
  - keep current default behavior when no date range is provided.

### Optional

- `/api/admin/export/users`
- `/api/admin/export/orders`
- `/api/admin/export/reviews`
- announcement fields and endpoints for pinned/effective window.

## Database

Required P0/P1 implementation can avoid schema changes.

Optional announcement governance may require a migration if existing `announcement` lacks fields:

- `pinned TINYINT(1) NOT NULL DEFAULT 0`
- `start_at DATETIME NULL`
- `end_at DATETIME NULL`

If optional fields are added, migration must be idempotent and safe to rerun.
### Decision 5: 公告置顶与有效期采用声明式设计

公告置顶和有效期是管理员后台治理的核心增强。公告列表需要对管理员展示完整的生命周期（置顶标志、有效期、状态联动），同时用户端只展示有效期内已发布的公告并按置顶优先排序。

```text
公告生命周期（管理端视角）：

  创建（DRAFT/PUBLISHED）
       │
       ├── 设置置顶标志 → 置顶优先展示
       │
       └── 设置有效期区间
              start_at                      end_at
               │                              │
               ▼                              ▼
         ┌─────────────┐              ┌──────────────┐
         │ 未生效       │   有效期内    │ 已过期        │
         │ (提前发布)   │──────────────▶│ (自动失效)    │
         └─────────────┘              └──────────────┘
```

方案对比：

| 方案 | 复杂度 | 灵活性 | 选择 |
|------|--------|--------|------|
| 仅数据库字段 + 管理端手动控制 | 低 | 中 | ✓ |
| 定时任务自动过期 | 中 | 高 | 后续可选 |
| 基于状态机的自动化管理 | 高 | 高 | 不做，超出MVP范围 |

选择"数据库字段 + 手动控制"方案：管理员设置置顶标志和有效期，服务端在查询时自动过滤过期公告并优先展示置顶公告。不引入定时任务，降低部署复杂度。

### Decision 6: 管理端和用户端公告查询分离

用户端 `GET /api/announcement/list` 只返回当前有效期内（`start_at <= NOW() AND (end_at IS NULL OR end_at >= NOW())`）且 `status = 'PUBLISHED'` 的公告，按置顶优先、时间倒序排列。

管理端应有独立的管理列表接口 `GET /api/announcement/admin/list`，返回全部状态和全部时间范围的公告，支持管理员查看和管理过期及未生效的公告。

### Decision 7: 数据迁移必须幂等

新增 `pinned`、`start_at`、`end_at` 字段的迁移脚本使用 `ALTER TABLE ... ADD COLUMN` 并配合条件检查确保可重复运行。现有数据保留原样：已有公告 `pinned=0`，`start_at=create_time`，`end_at=NULL`。


## Conflict Boundaries

成员 C 可以修改：

- `frontend/src/views/AdminAuditView.vue`
- `frontend/src/views/admin/AdminDashboardView.vue`
- `frontend/src/views/admin/AdminUsersView.vue`
- `frontend/src/views/admin/AdminOrdersView.vue`
- `frontend/src/views/admin/AdminReviewsView.vue`
- `frontend/src/views/admin/AdminAnnouncementsView.vue`
- `frontend/src/api/admin.js`
- `frontend/src/api/clas.js`
- `backend/src/main/java/com/clas/controller/AdminController.java`
- `backend/src/main/java/com/clas/controller/MerchantController.java`
- `backend/src/main/java/com/clas/service/StatisticsService.java`
- relevant DTOs/services only when needed for admin display or governance.

成员 C 应避免修改：

- 用户端页面，如 `HomeView.vue`、`ProfileView.vue`、`OrdersView.vue`。
- 商家端业务页，如 `MerchantConsoleView.vue`、`MerchantProductsView.vue`、`MerchantBookingsView.vue`。
- 下单、支付、退款、接单的核心交易服务。
- 大范围全局 CSS 和路由结构，除非仅为后台入口或构建修复。

## Risks / Trade-offs

- [Risk] 后台页面乱码可能来自历史文件编码问题，修复范围较大。  
  Mitigation: 限定在 C 包后台页面和相关提示文案，避免全仓格式化。
- [Risk] 数据看板日期筛选可能改变统计口径。  
  Mitigation: 无参数时保持当前默认统计，新增参数只影响筛选查询。
- [Risk] 导出接口增加范围。  
  Mitigation: 标为可选，先完成筛选和治理闭环。
- [Risk] 公告置顶/有效期需要数据库字段。  
  Mitigation: 可选实现；若做，迁移必须幂等。
- [Risk] 与其他成员改 `api/clas.js` 或路由冲突。  
  Mitigation: 优先新增 `api/admin.js` 辅助方法，减少 shared file 修改。

## Verification Plan

- 前端：
  - `cd frontend && npm run build`
  - 手工检查 `/admin/audit`、`/admin/dashboard`、`/admin/users`、`/admin/orders`、`/admin/reviews`、`/admin/announcements`
- 后端：
  - `cd backend && mvn test`
  - 如测试依赖环境不足，至少执行 `mvn clean package -DskipTests`
- 服务器：
  - `clas deploy`
  - `curl http://127.0.0.1/api/health`
  - 浏览器访问 `http://8.141.112.182/admin/dashboard`
