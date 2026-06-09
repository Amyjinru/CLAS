## Context

CLAS 当前已经具备生活服务平台的主体形态：用户端可浏览商家、管理地址、收藏、下单、支付、评价、团购和预约；商家端可入驻、管理商品、处理订单、管理团购和预约；管理员端可做商家审核、用户/订单/评价/公告管理和数据看板。

本路线图面向“功能迭代”，不是最终文档收口。优先级按产品价值和课程展示价值划分：

- P0 核心功能：没有它，生活服务平台闭环不完整或演示不稳定。
- P1 常见商业功能：真实商业平台常见，可以提升完整度和业务可信度。
- P2 项目亮点功能：有助于课程优秀、答辩亮眼，但不应影响 P0/P1 稳定性。

状态定义：

- 已实现：代码中已有对应后端、前端或数据库主流程。
- 待完善：已有基础能力，但流程、体验、安全、数据模型或测试还不够完整。
- 未实现：当前项目中未看到明确对应模块或只有概念占位。

## Goals / Non-Goals

**Goals:**

- Provide a complete P0/P1/P2 product roadmap for CLAS.
- Identify implemented, partially implemented, and missing functions.
- Describe the exact improvement work for partially implemented and missing functions.
- Keep the roadmap realistic for a Spring Boot + MySQL + Vue3 course project.
- Help the team choose the next iteration without over-expanding scope.

**Non-Goals:**

- Implement these functions in this proposal.
- Require production-grade payment, logistics, SMS, recommendation, or AI integrations.
- Replace the current application stack.
- Turn CLAS into a full Meituan-scale platform.

## Decisions

### Decision 1: P0 focuses on closed-loop reliability

P0 is not “all basic CRUD”; it is the set of features required to make USER, MERCHANT, and ADMIN flows complete and stable. The current CLAS already has many P0 features, so most P0 work is polishing and consistency.

### Decision 2: P1 focuses on commercial realism

P1 includes coupons, merchant operations, platform configuration, settlement, customer service, and search/discovery improvements. These are common in commercial life service platforms but can be implemented in simplified form.

### Decision 3: P2 focuses on standout but bounded highlights

P2 should be chosen carefully: map routing, analytics, recommendation, security upgrade, API automation, and intelligent assistant are valuable, but only one or two should be implemented deeply enough to demonstrate quality.

### Decision 4: Route by business value, not technical novelty

The roadmap prioritizes features that make the platform explainable in a course demo. Pure infrastructure upgrades are included only when they improve trust, safety, or maintainability.

## P0 核心功能

| 功能 | 状态 | 当前依据 | 待完善/未实现具体内容 |
| --- | --- | --- | --- |
| 用户注册登录 | 待完善 | `UserController`、`LoginView`、`ForgotPasswordView`、Redis 验证码 | 密码加密；登录失败次数限制；验证码过期提示；统一账号口径；登录态过期处理 |
| 角色权限体系 | 待完善 | `AuthInterceptor`、`@RequireRole`、前端路由 meta | 当前为 `Authorization: phone` 演示鉴权；需区分 401/403；可选 JWT/Spring Security；路由 public/role 规则需统一 |
| 商家列表与详情 | 待完善 | `MerchantController`、`HomeView`、`MerchantDetailView` | 补充分类导航、距离排序、营业/休息状态展示、空结果页、商家图片/标签 |
| 商品浏览 | 待完善 | `ProductController`、商品表、商家详情页 | 商品分类、规格/属性、售罄状态、图片兜底、库存不足提示 |
| 购物车 | 待完善 | `CartController`、`CartView`、`cart` 表 | 同一购物车限制单商家或多商家分组；库存变动校验；失效商品提醒；批量删除 |
| 创建订单 | 待完善 | `OrderController`、`orders/order_item` 表 | 订单快照信息不足；下单事务与库存扣减需明确；配送费/起送价校验；订单备注 |
| 模拟支付 | 待完善 | `PaymentController`、`payment` 表、`PaymentView` | 支付超时、重复支付幂等、支付失败模拟、退款流水关联 |
| 商家接单与履约 | 待完善 | `MerchantConsoleView`、订单状态和配送状态 | 状态机可视化；拒单理由；配送中/已送达细分；商家超时提醒 |
| 用户确认完成 | 已实现 | 订单完成和评价入口已存在 | 可补确认弹窗、完成后推荐评价/复购入口 |
| 评价与评分 | 待完善 | `ReviewController`、`review` 表、`ReviewView` | 评价图片、匿名评价、评分维度、追评、差评处理流程 |
| 商家入驻 | 待完善 | `MerchantRegisterView`、`MerchantController`、`merchant` 表 | 资质材料上传、审核进度页、入驻失败修改再提交、商家协议勾选 |
| 管理员商家审核 | 待完善 | `AdminAuditView`、`merchant_audit_log` 表 | 审核筛选、审核详情页、资料预览、批量处理、审核通知 |
| 用户地址管理 | 待完善 | `AddressController`、`user_address` 表、`ProfileView` | 地址编辑、地图选点稳定性、地址标签、默认地址唯一性数据库保障 |
| 订单列表与详情 | 待完善 | `OrdersView`、`OrderDetailContent` | 统一订单详情页；状态时间轴；售后入口；团购/普通订单区分 |
| 公告通知 | 待完善 | `AnnouncementController`、`NotificationController` | 通知分类、全部已读、未读数角标、公告有效期和置顶 |
| 基础数据看板 | 待完善 | `AdminDashboardView`、`StatisticsService` | 指标定义说明；日期筛选；导出；空数据展示 |

P0 建议顺序：

```text
账号/权限口径
  -> 下单支付履约闭环
  -> 商家入驻审核闭环
  -> 订单/评价/通知闭环
  -> 演示数据和测试用例稳定
```

## P1 常见商业功能

| 功能 | 状态 | 当前依据 | 待完善/未实现具体内容 |
| --- | --- | --- | --- |
| 优惠券/满减活动 | 未实现 | 目前只有团购券，没有通用优惠券 | 新增 coupon/coupon_user；支持满减、折扣、有效期、领取、下单抵扣 |
| 店铺营销活动 | 待完善 | `group_deal` 已有团购 | 增加限时折扣、套餐、店铺满减、活动上下架、活动与商品关联 |
| 团购券体系 | 待完善 | `DealController`、`group_deal/deal_order` | 退款、过期自动失效、核销记录、券详情页、使用规则 |
| 搜索与筛选增强 | 待完善 | 首页已有搜索/分类/排序基础 | 综合搜索商家和商品；价格区间；销量排序；距离排序；历史搜索 |
| 商品分类与规格 | 未实现 | `product` 无 category/spec 表 | 商家自定义分类；规格选项；多规格价格和库存 |
| 商家营业管理 | 待完善 | `merchant.status`、营业时间字段 | 商家自主开关店；节假日营业；超出营业时间禁止下单 |
| 配送规则 | 待完善 | 配送半径、配送费、距离字段已有 | 按距离计算配送费；超配送范围限制；预计送达时间；配送费减免 |
| 售后退款 | 待完善 | `refund_status/refund_reason` 已有 | 退款原因分类；退款凭证；退款进度；管理员介入；退款流水 |
| 客服/工单 | 未实现 | 无客服表和入口 | 用户发起咨询/投诉；商家或管理员回复；工单状态；消息通知 |
| 收藏与关注 | 待完善 | `FavoriteController`、`favorite` 表 | 收藏分组；收藏店铺动态；取消确认；首页快捷入口 |
| 用户会员体系 | 未实现 | 无会员等级/积分表 | 积分、等级、成长值、会员权益、签到或消费返积分 |
| 商家结算 | 未实现 | merchant 有银行账户和结算周期字段 | 订单收入统计、平台抽佣、待结算/已结算状态、结算记录 |
| 商家经营分析 | 待完善 | 管理端有平台统计，商家端分析较弱 | 商家销售额、订单趋势、热销商品、评价趋势、退款率 |
| 管理员运营配置 | 未实现 | 公告管理已有，平台规则配置缺失 | 类目管理、公告置顶、平台费率、配送基础规则、敏感词 |
| 内容治理 | 待完善 | 评价举报已有 | 商家资质治理、商品违规下架、公告审核、举报证据 |
| 数据导出 | 未实现 | 无导出接口 | 管理员导出订单、用户、商家、评价；CSV/XLSX 二选一即可 |
| 多端适配 | 待完善 | Vue 页面已有，移动端未明确验收 | 核心用户端页面移动端适配；管理后台桌面优先；截图验收 |

P1 建议顺序：

```text
优惠券/营销
  -> 搜索发现增强
  -> 配送/售后规则
  -> 商家经营分析
  -> 平台运营配置
```

## P2 项目亮点功能

| 功能 | 状态 | 当前依据 | 待完善/未实现具体内容 |
| --- | --- | --- | --- |
| 地图定位与路线配送 | 待完善 | `AmapRouteService`、地图组件、经纬度/距离字段 | 在下单和商家详情中稳定展示定位、距离、路线、预计时间；无 Key 时降级 |
| 智能推荐 | 未实现 | 当前排序主要基于评分/价格/最新 | 基于收藏、订单、类别偏好做简单推荐；不必上复杂算法 |
| 智能搜索 | 未实现 | 仅基础 keyword 搜索 | 支持关键词联想、热门搜索、错别字弱匹配、分类建议 |
| AI 客服/智能问答 | 未实现 | 无对应模块 | 课程可做规则型 FAQ 或本地知识库问答；回答订单/退款/入驻问题 |
| 实时通知 | 未实现 | 当前为站内通知轮询/列表 | WebSocket 或 SSE 推送订单状态、审核结果、退款结果 |
| 可视化运营大屏 | 待完善 | 管理端 ECharts 基础图表 | 大屏模式；实时订单、销售额、商家排行、地图热力展示 |
| 安全升级 | 待完善 | 当前演示鉴权 | JWT/Spring Security、BCrypt、接口 401/403、登录过期、密码重置安全 |
| 自动化测试体系 | 待完善 | Maven 测试、测试文档已有 | 核心 Service 集成测试；API 集合；前端构建；E2E 主流程 smoke test |
| 数据库迁移体系 | 待完善 | schema + migration 脚本已有 | Flyway 或版本化 migrations；变更编号；回滚说明 |
| 推荐实验/AB 配置 | 未实现 | 无实验配置 | 作为亮点可用简单配置表控制首页排序策略 |
| 审计日志体系 | 待完善 | 商家审核日志已有 | 扩展到管理员禁用用户、删除评价、公告操作、退款介入 |
| 文件上传与对象存储 | 未实现 | 图片路径字段已有，缺上传流程 | 商品图、商家资质、评价图上传；本地存储即可课程演示 |
| 消息模板中心 | 未实现 | 通知内容散落在业务流程 | 通知模板表；按事件生成通知；便于扩展短信/邮件 |
| 开放接口文档 | 未实现 | README 有接口摘要 | Swagger/OpenAPI 或 Apifox 文档导出；答辩展示更工程化 |

P2 建议选型：

```text
优先组合 A：地图配送 + 运营大屏
  适合答辩展示，视觉效果强，和生活服务平台贴合。

优先组合 B：安全升级 + 自动化测试
  适合体现软件工程规范，风险中等。

优先组合 C：智能推荐 + AI 客服
  亮点强，但容易做浅。只有 P0/P1 稳定后再做。
```

## Recommended Iteration Plan

### Iteration 1: P0 闭环稳定

- 完善登录/权限错误码和演示账号口径。
- 稳定用户下单、支付、商家履约、评价闭环。
- 补订单详情、状态反馈、空状态和失败提示。
- 固化演示数据和测试流程。

### Iteration 2: P0 商家与管理员闭环

- 完善商家入驻资料、审核详情和审核通知。
- 完善商品管理、订单处理、退款审核、预约处理。
- 完善管理员商家审核、评价治理、公告管理和数据看板。

### Iteration 3: P1 商业化增强

- 实现优惠券/满减或增强团购券。
- 增强搜索、分类、排序和配送规则。
- 增加商家经营分析和平台运营配置。

### Iteration 4: P2 亮点冲刺

- 从地图配送、运营大屏、安全升级、自动化测试、智能推荐中选择 1-2 个做深。
- 补充截图、测试用例和答辩讲解材料。

### Iteration 5: Final Hardening

- 清理历史文档口径。
- 运行完整测试。
- 修复演示阻塞问题。
- 打包最终课程提交物。

## Risks / Trade-offs

- [Risk] P1/P2 功能过多导致 P0 不稳定 -> Mitigation: P0 未验收前不进入大规模 P1/P2 开发。
- [Risk] 亮点功能做得浅反而扣分 -> Mitigation: P2 只选 1-2 个深做，并配测试和演示材料。
- [Risk] 引入 JWT/Flyway/WebSocket 等新技术造成回归 -> Mitigation: 作为独立迭代，先保留当前演示路径。
- [Risk] 数据库改动影响已有测试数据 -> Mitigation: 每次 schema 改动同步 migration、H2 schema 和 seed data。
- [Risk] 文档跟不上功能迭代 -> Mitigation: 每个功能任务必须包含测试记录和用户手册更新。

## Migration Plan

This roadmap proposal does not change runtime behavior.

For future iterations:

1. Select one iteration scope at a time.
2. Create a specific OpenSpec change for that iteration.
3. Update schema/migration/test schema together for data model changes.
4. Add or update test cases before marking the iteration complete.
5. Update user manual, deployment notes, and test report after each visible feature.
