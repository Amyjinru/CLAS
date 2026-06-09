## Why

成员 C 负责“管理后台与平台治理包”。当前 CLAS 已有管理员后台基础入口，包括商家审核、仪表盘、用户管理、订单管理、评价治理、公告管理和申诉处理，但后台仍存在三类问题：

- 部分页面中文文案显示为乱码，影响课程演示和验收。
- 商家审核缺少足够的筛选、详情、审核记录和备注回看能力，管理员处理风险商家时上下文不足。
- 管理后台的数据看板、运营筛选、导出和治理操作还偏基础，无法体现平台治理闭环。

本变更为成员 C 提供聚焦后台的增强计划，优先保障 P0 商家审核闭环和后台可演示性，再补齐数据看板和运营治理能力。边界上不改用户端、商家端业务页面，不介入交易核心逻辑，降低与其他成员的冲突。

## What Changes

- 修复管理后台相关页面的乱码文案，使后台页面可正常中文演示。
- 增强 `AdminAuditView`：商家状态筛选、关键词筛选、详情抽屉/弹窗、审核日志展示、备注回显、无数据/补审核提示。
- 完善商家审核后端：审核日志查询、审核备注返回、商家详情返回字段和状态统计可读性。
- 增强 `AdminDashboardView`：日期范围筛选、图表空状态、刷新状态、大屏模式可选；后端统计接口支持只读筛选。
- 增强管理员运营治理页面：用户/订单/评价/公告筛选与导出，评价举报处理状态闭环。

  **公告管理增强为独立板块**：
  - 公告表单增加置顶开关和有效期选择器，列表展示置顶标识和有效期时间线。
  - 用户端公告接口自动过滤过期公告，置顶公告优先展示。
  - 管理端公告列表展示全部状态（包括过期/未生效），支持管理员全生命周期管控。
  - 数据库迁移：announcement 表新增 pinned、start_at、end_at 字段，迁移幂等可重复运行。
- 保持治理接口只读或轻量状态更新，避免改动支付、下单、商家接单等交易流程。

## Capabilities

### New Capabilities

- `admin-governance-package`: 管理员可以在后台完成商家审核、数据观察、用户/订单/评价/公告治理的集中处理，并获得清晰的筛选、详情、状态和反馈。

### Modified Capabilities

- `merchant-audit-admin`: 增强商家审核列表、详情、日志和备注能力。
- `admin-dashboard`: 增强后台看板筛选、图表空状态和大屏展示能力。
- `admin-operations-governance`: 增强用户、订单、评价、公告的运营治理操作。

## Impact

- Frontend:
  - `frontend/src/views/AdminAuditView.vue`
  - `frontend/src/views/admin/AdminDashboardView.vue`
  - `frontend/src/views/admin/AdminUsersView.vue`
  - `frontend/src/views/admin/AdminOrdersView.vue`
  - `frontend/src/views/admin/AdminReviewsView.vue`
  - `frontend/src/views/admin/AdminAnnouncementsView.vue`
  - possibly `frontend/src/api/admin.js`, `frontend/src/api/clas.js`, `frontend/src/router/index.js`
- Backend:
  - `backend/src/main/java/com/clas/controller/AdminController.java`
  - `backend/src/main/java/com/clas/controller/MerchantController.java`
  - `backend/src/main/java/com/clas/service/StatisticsService.java`
  - possibly review, announcement, merchant DTO/service helpers
- Database:
  - Required schema migration for announcement `pinned`, `start_at`, `end_at` fields. Migration must be idempotent.
  - Update `database/schema.sql` with new columns.
  - Update test schema if applicable.
- Conflict boundary:
  - Do not edit user portal pages, merchant console business flows, cart/order creation/payment logic, or core transaction services unless a compile fix is required.
