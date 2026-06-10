## Context

CLAS 当前采用 Spring Boot + MyBatis-Plus + MySQL 后端、Vue + Vite + Element Plus 前端。审查发现安全、交易、查询、数据和前端结构风险相互影响：认证兼容逻辑会绕过 JWT；支付和优惠券缺少清晰状态机；统计接口依赖全表加载；数据库缺少完整性约束；前端页面承担了过多业务、格式化和 UI 编排职责。

本设计按风险优先级推进：先关闭可被直接利用的认证漏洞，再收敛交易状态和并发更新，然后治理查询性能和数据完整性，最后拆分前端页面和构建体积。实现必须保持现有核心业务可用，并通过迁移脚本、兼容期接口和集成测试降低回归风险。

## Goals / Non-Goals

**Goals:**

- 移除手机号伪认证，仅允许 Bearer JWT，并强制安全 JWT secret 配置。
- 建立支付、库存、订单和优惠券的原子状态转换模型。
- 将后台统计和高频详情查询从全表/逐条查询改为 SQL 聚合和批量查询。
- 补充数据库完整性保护，明确哪些关系使用外键，哪些关系使用应用级约束。
- 收敛 API 边界，减少无效 `userId` 参数和跨角色接口混杂。
- 抽取前端通用工具、composables 和组件，降低大页面维护成本并优化分包。

**Non-Goals:**

- 不接入真实第三方支付渠道，本变更只把现有 mock 支付改造成可替换的状态模型。
- 不重新设计 CLAS 的业务域模型或 UI 视觉风格。
- 不一次性删除所有旧 API；旧 API 可保留短期兼容，但必须转发到新实现并标记废弃。
- 不把所有统计改造成实时数仓或缓存体系；优先使用数据库聚合和索引。

## Decisions

### Decision 1: 认证只接受 Bearer JWT

后端 `AuthInterceptor` MUST 拒绝非 `Bearer ` 开头的 `Authorization` 值。前端请求拦截器只在存在 token 时发送 Bearer header，不再 fallback 到 phone。

Alternative considered: 保留 phone 兼容并增加配置开关。拒绝该方案，因为默认或误开兼容都会继续保留伪造身份风险。

### Decision 2: JWT secret 在应用启动时 fail-fast

`JwtUtil` 或独立配置校验组件 MUST 在非 test profile 下拒绝缺失、默认开发值和过短 secret。测试环境可以使用专用 test secret，以保证集成测试稳定。

Alternative considered: 仅在 README 中提示配置。拒绝该方案，因为配置遗漏是高概率部署错误，必须由程序阻断。

### Decision 3: 支付状态流转使用条件更新和短事务

支付流程拆为创建支付记录、确认支付结果、扣库存/占用优惠券、更新订单状态几个小步骤。任何订单状态更新 MUST 带当前状态条件，例如 `WHERE id = ? AND status = 'PENDING_PAYMENT'`；扣库存继续使用 `stock >= quantity` 条件更新。模拟支付延迟必须在事务外执行。

Alternative considered: 保留当前单方法事务并只调整顺序。拒绝该方案，因为长事务会占用连接并阻碍未来替换真实支付回调。

### Decision 4: 优惠券引入 RESERVED 状态

领取优惠券时通过条件 SQL 原子增加 `claimed_count`。创建订单时将 `UNUSED` 优惠券条件更新为 `RESERVED` 并写入 `order_id`；支付成功改为 `USED`；订单取消、支付失败或超时释放回 `UNUSED`。状态变化必须验证 `user_id`、`status` 和 `order_id`。

Alternative considered: 只在业务层加 synchronized 或事务隔离级别。拒绝该方案，因为多实例部署下 JVM 锁无效，高隔离级别也会扩大锁范围。

### Decision 5: 统计和详情优先使用 mapper 聚合查询

后台统计使用 SQL `COUNT`, `SUM`, `GROUP BY`, `ORDER BY`, `LIMIT` 返回 DTO；订单列表和评价详情使用批量 `IN` 查询后按 id 分组组装，避免每条记录单独查关联数据。

Alternative considered: 引入 Redis 缓存统计。暂不采用，因为当前瓶颈首先来自查询形态，缓存会增加一致性复杂度。

### Decision 6: 数据完整性采用外键优先、应用级约束补充

核心强关系优先补充外键：订单明细到订单/商品、支付到订单、评价到订单、优惠券到券定义、商家到用户。涉及历史数据或软删除冲突的关系，可先建立应用级完整性检查、孤儿数据清理任务和测试，再在数据清理后补外键。

Alternative considered: 永久无外键。拒绝作为默认策略，因为系统已有多处跨表删除和状态流转，缺少约束会积累不可见数据损坏。

### Decision 7: 前端先抽横向基础设施，再拆大页面

先抽 `formatFen`, `formatTime`, status maps, `useTableQuery`, `useConfirmAction`, `MoneyText`, `StatusTag`, `AdminDataTable` 等稳定公共层，再拆 `ProfileView`, `MerchantDetailView`, `MerchantProductsView`, `CartView` 的局部业务组件。构建优化以动态路由和 `manualChunks` 为主，ECharts 只在后台图表页加载。

Alternative considered: 直接按页面重写。拒绝该方案，因为大范围 UI 重写会放大回归风险。

## Risks / Trade-offs

- [Risk] 移除 phone 认证后旧本地会话失效 -> [Mitigation] 前端 401 自动清理会话并跳转登录，发布说明标记为破坏性变更。
- [Risk] 条件更新引入更多失败分支 -> [Mitigation] 为重复支付、库存不足、优惠券已占用、取消释放写集成测试。
- [Risk] 外键迁移遇到历史孤儿数据失败 -> [Mitigation] 先提供检测 SQL 和清理迁移，再加约束；无法一次性清理的关系先走应用级约束。
- [Risk] SQL 聚合与原内存统计口径不一致 -> [Mitigation] 在测试数据上比较旧口径与新口径，明确排除 `PENDING_PAYMENT` 等状态。
- [Risk] 前端拆分过程中出现样式/交互回归 -> [Mitigation] 每拆一个页面保留原 API 行为，运行 build，并对关键页面做手动 smoke checklist。

## Migration Plan

1. 发布认证安全修复：移除 phone fallback，要求 JWT secret，更新前端 header，补安全回归测试。
2. 发布交易状态修复：新增 mapper 条件更新，改造支付与优惠券状态机，补迁移脚本和并发测试。
3. 发布查询性能修复：重写统计聚合、订单/评价批量查询，补索引迁移。
4. 发布数据完整性迁移：先检测/清理孤儿数据，再补外键或应用级完整性检查。
5. 发布 API 收敛和前端瘦身：新增 me 路径，前端切换到新 API，旧路径进入废弃期；抽取共享工具和组件。
6. 发布构建优化：配置 manual chunks，ECharts 按需/懒加载，确认 chunk warning 降低。

Rollback strategy: 认证变更只能通过重新登录恢复，不恢复 phone fallback；交易/SQL/API/前端变更通过保留旧接口转发、数据库迁移可回滚脚本和小批次提交降低回滚范围。

## Open Questions

- 生产环境 profile 命名是否固定为 `prod`，还是用“非 test 即严格”的策略？
- 外键是否允许对历史演示数据做一次性清理，还是需要长期保持无外键兼容？
- 旧 API 的废弃窗口是一个版本、两个版本，还是本次实现内前端全部切换后立即删除？
