## Why

CLAS 已具备主要业务流程，但近期审查发现核心风险集中在认证遗留、交易一致性、后台查询性能、数据完整性和前端可维护性上。这些问题会直接影响账号安全、订单/优惠券正确性、数据增长后的可用性，以及后续功能迭代速度，因此需要一次面向核心风险的治理变更。

## What Changes

- **BREAKING**: 后端只接受 `Authorization: Bearer <JWT>`，移除 `Authorization: <phone>` 兼容认证；前端不再发送手机号作为认证头。
- **BREAKING**: 启动时强制校验 `JWT_SECRET`，生产和非测试环境不得使用默认开发密钥或过短密钥。
- 重构支付流程，将模拟等待移出数据库事务，将支付创建、支付结果确认、库存扣减、订单状态流转拆成可恢复的步骤，并使用条件更新保护状态转换。
- 优惠券领取和订单预占改为原子更新，引入 `RESERVED` 状态，避免并发超发和同一券被多个订单占用。
- 后台统计改为 SQL 聚合，减少全表加载和内存聚合；补充必要组合索引。
- 订单列表和评价详情改为批量查询/组装，消除主要 N+1 查询。
- 建立数据库完整性策略：优先补充外键与索引；对无法加外键的表建立应用级约束、清理任务和集成测试。
- 收敛 REST API 设计，新增/迁移到 `/api/users/me/...`、`/api/merchant/me/...`、`/api/admin/...`，逐步移除无效 `userId` 路径参数。
- 拆分超大前端页面，提取通用 composables、格式化工具和复用组件；优化 Vite manual chunks 与 ECharts 按需加载。

## Capabilities

### New Capabilities

- `strict-bearer-auth`: 严格 Bearer JWT 认证、JWT secret 启动校验、前端认证头清理。
- `transaction-consistency`: 支付、库存、订单状态和优惠券状态的原子一致性保障。
- `analytics-query-performance`: 后台统计 SQL 聚合、列表/详情批量查询和性能索引。
- `data-integrity-governance`: 数据库外键/索引策略、应用级完整性约束、孤儿数据预防与检测。
- `api-surface-governance`: 面向当前用户/商户/管理员的 REST API 边界收敛和旧接口兼容策略。
- `frontend-maintainability`: 前端页面瘦身、通用组件/工具抽取、构建分包优化。

### Modified Capabilities

<!-- 当前仓库没有已归档的 openspec/specs 主规格；本变更以新增能力定义核心风险治理合同。 -->

## Impact

- **Backend auth**: `AuthInterceptor`, `JwtUtil`, `application.yml`, 登录/会话相关测试。
- **Backend transaction**: `PaymentService`, `OrderService`, `CouponService`, `ProductMapper`, `OrdersMapper`, `PaymentMapper`, `CouponMapper`, 交易状态相关 DTO/测试。
- **Backend performance**: `StatisticsService`, `OrderService.withItems`, `ReviewService.toDetail`, admin 列表接口和 mapper 聚合查询。
- **Database**: `database/schema.sql`, migration scripts, test schema, indexes, optional foreign keys and integrity checks.
- **API clients**: `frontend/src/api/client.js`, `session.js`, `cart.js`, `order.js`, product/admin API wrappers and route consumers.
- **Frontend UI**: `ProfileView.vue`, `MerchantDetailView.vue`, `MerchantProductsView.vue`, `CartView.vue`, shared components/composables/utils, `vite.config.js`.
- **Tests**: Add security regression tests, concurrent coupon/payment tests, query-shape tests where practical, frontend build verification.
