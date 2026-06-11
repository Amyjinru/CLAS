## Context

普通用户端当前已经具备大部分业务页面和 API：

- `App.vue` 中普通用户导航仍包含“消息、我的、购物车、订单、商家入驻”等分散入口。
- `router/index.js` 已有 `/home`、`/deals`、`/bookings`、`/profile`、`/cart`、`/orders`、`/profile/notifications` 等用户路由。
- `ProfileView.vue` 已经加载头像/昵称、地址、收藏、团购券、通知、处罚/申诉数据，并拆出了 `ProfileHero`、`ProfileSummary`、`ProfileAddressSection`、`ProfilePenaltySection`。
- `OrdersView.vue` 已经展示订单状态、退款信息、评价入口和商家聊天，可以复用其订单生命周期判断。
- `profile.js` 已有头像、昵称、手机号验证码和手机号变更 API；地址、收藏、通知、购物车、优惠券/团购券也已有独立 API 模块。
- 当前缺口主要是信息架构、设置页、安全设置中的密码修改、银行卡管理，以及通用设置偏好。

## Goals / Non-Goals

**Goals:**

- 将普通用户顶部导航调整为外卖、团购、预订/到店、个人中心、设置。
- 复用现有页面、组件、API 和 CSS 变量，保持前端视觉风格一致。
- 将个人中心重组为订单、购物车、收藏、券包、消息五个清晰板块。
- 将设置页设计为独立用户设置工作台，包含个人信息、收货地址、账号安全、支付设置、通用设置。
- 补齐必要的最小后端能力：密码修改、银行卡绑定/列表/删除。
- 银行卡展示遵循主流支付产品做法：卡片化列表、银行/尾号/持卡人摘要、添加弹窗、删除确认、全卡号不回显。

**Non-Goals:**

- 不接入真实银行卡清算、绑卡鉴权、支付网关或三方支付 SDK。
- 不重做订单、购物车、收藏、优惠券、通知的业务模型。
- 不改商家端、管理后台导航。
- 不做完整多语言文案体系；本次只实现语言偏好与核心导航/设置标签的可切换基础。
- 不把用户端设置页做成营销落地页；保持当前后台式、信息密度适中的工具界面。

## Decisions

### Decision 1: 导航只重组普通用户入口

在 `App.vue` 中将 `userPrimaryNav` 调整为：

```js
[
  { label: '外卖', to: '/home' },
  { label: '团购', to: '/deals' },
  { label: '预订/到店', to: '/bookings' },
  { label: '个人中心', to: '/profile' },
  { label: '设置', to: '/settings' }
]
```

移除普通用户顶部的购物车、订单、消息等二级工具入口，把它们集中放入个人中心。商家入驻如仍需保留，可在首页或个人中心低优先级入口展示，不再作为普通用户顶栏大分类。

备选方案：继续在顶栏显示购物车/订单/消息。暂不采用，因为用户明确要求五个大分类，且这些能力更适合作为个人中心模块入口。

### Decision 2: 个人中心从“资料页”升级为“用户中心仪表盘”

在 `ProfileView.vue` 中保留 `ProfileHero` 和现有加载逻辑，但将 tab/section 重组为：

- 订单：状态模块卡片和最近订单摘要，点击进入 `/orders` 或 `/orders?tab=...`。
- 购物车：购物车数量、待支付提示、进入 `/cart`。
- 收藏：复用 `listFavorites` 与 `removeFavorite`。
- 券包：复用 `listMyDealOrders`，并接入 `listMyCoupons` 或现有 coupon API 可用数据。
- 消息：复用 `listNotifications`、单条已读、全部已读、清空通知和公告入口。

如果实现阶段发现 `ProfileView.vue` 继续膨胀，应按现有 `components/profile/` 模式拆分为 `ProfileOrderBlock`、`ProfileCartBlock`、`ProfileFavoritesBlock`、`ProfileVoucherBlock`、`ProfileMessageBlock`。

备选方案：为五个板块都新建完整页面。暂不采用，因为已有独立页面能承载详细工作流，个人中心更适合作为聚合入口。

### Decision 3: 订单分组复用状态映射，不新增订单模型

订单模块按现有状态和字段派生：

- 全部订单：所有 `listOrders()` 返回项。
- 待收货/使用：`PAID`、`ACCEPTED`、配送中相关 `deliveryStatus`，以及未来到店券未使用状态。
- 待评价：`COMPLETED` 且 `getReviewByOrder` 无结果。
- 退款/售后：`REFUND_REQUESTED`、`REFUNDED`、`refundStatus !== 'NONE'` 或可申请退款的已支付/已完成订单。
- 额外状态摘要：建议使用“待支付”，覆盖 `PENDING_PAYMENT` 食品订单和团购待支付订单。

备选方案：新增后端订单聚合接口。可以后续优化，但本次前端已有足够数据源，先避免扩大后端改动。

### Decision 4: 设置页独立成 `UserSettingsView.vue`

新增 `/settings` 路由与 `UserSettingsView.vue`，页面采用与 `ProfileView.vue` 一致的 `user-page`、`panel`、Element Plus tabs/form/dialog 样式。五个模块建议使用左侧锚点/标签页或 Element Plus tabs：

- 个人信息：复用 `ProfileHero` 的头像上传和昵称表单，或抽取为共享 `ProfileInfoPanel`。
- 收货地址：直接复用 `ProfileAddressSection`，避免复制地址表单。
- 账号安全：手机号修改复用 `sendPhoneChangeCode`、`updateBoundPhone`；密码修改新增 API。
- 支付设置：新增银行卡卡片列表和添加弹窗。
- 通用设置：语言选择、主题切换。

备选方案：继续把所有设置放到个人中心 tabs。暂不采用，因为“个人中心”和“设置”已经被要求为顶层分类，且设置项包含安全/支付，独立页面更清晰。

### Decision 5: 密码修改新增最小安全接口

新增 DTO 和接口：

- `PasswordChangeRequest(currentPassword, newPassword, confirmPassword)`
- `PUT /api/user/password`
- 服务层校验当前用户存在、账号未禁用、当前密码正确、新密码满足 `PasswordValidator`、两次新密码一致。
- 使用现有 `PasswordEncoderConfig`/密码编码方式更新 `User.password`。

备选方案：复用忘记密码接口。暂不采用，因为设置页修改密码必须验证当前密码，不应通过短信重置流程绕过。

### Decision 6: 银行卡管理新增独立表和脱敏 DTO

新增轻量数据模型 `user_bank_card`：

- `id`
- `user_id`
- `bank_name`
- `cardholder_name`
- `card_no_encrypted` 或在课程项目中至少避免明文响应
- `card_last4`
- `card_type`
- `is_default`
- `create_time`

新增接口：

- `GET /api/user/bank-cards`
- `POST /api/user/bank-cards`
- `DELETE /api/user/bank-cards/{id}`

响应只返回 `id`、`bankName`、`cardholderName`、`maskedCardNo`、`cardLast4`、`cardType`、`isDefault`、`createTime`。删除和查询必须按 `UserContext.getUserId()` 做归属过滤。

备选方案：把银行卡信息塞进现有 `payment` 表。暂不采用，因为支付流水和绑卡资料生命周期不同，混用会让查询和权限控制变乱。

### Decision 7: 通用设置先前端持久化

主题和语言先通过 localStorage 保存，例如 `clas-theme-mode`、`clas-language`。主题切换通过给 `document.documentElement` 添加 `data-theme="dark"` 或 class，再在 `theme.css` 中补充暗色变量。语言偏好先覆盖顶部导航和设置页核心标签；后续如果项目需要完整国际化，再引入 i18n。

备选方案：立即新增用户偏好表。暂不采用，因为需求没有要求跨设备同步，前端偏好足够支撑本次体验。

## Risks / Trade-offs

- [Risk] `ProfileView.vue` 继续变大影响维护性 -> Mitigation: 按 `frontend-maintainability` 规格拆分五个 block 组件，保留页面只做数据聚合和布局。
- [Risk] 订单分组需要额外调用 `getReviewByOrder`，订单多时请求较多 -> Mitigation: 先沿用当前 `OrdersView.vue` 做法；后续可加批量评价状态接口。
- [Risk] 银行卡信息安全要求高 -> Mitigation: 本次不接真实支付，只做教学项目级绑定管理；API 永不返回完整卡号，删除/查询严格做用户归属校验。
- [Risk] 暗色模式可能暴露局部硬编码颜色 -> Mitigation: 新样式优先使用 `theme.css` CSS 变量，发现硬编码时局部替换。
- [Risk] 顶栏移除购物车/订单可能降低直达效率 -> Mitigation: 在个人中心放置醒目的购物车和订单模块，并允许首页/商家页保留上下文内购物车入口。

## Migration Plan

1. 新增 `/settings` 路由和空壳 `UserSettingsView.vue`，确认普通用户可访问。
2. 调整 `App.vue` 普通用户导航为五个大分类。
3. 重构 `ProfileView.vue` 为五大板块，必要时拆分 profile block 组件。
4. 增强 `OrdersView.vue` 支持 query/tab 过滤，或在个人中心通过链接进入现有订单页。
5. 实现设置页个人信息和收货地址模块，优先复用现有组件/API。
6. 新增密码修改后端接口和前端表单。
7. 新增银行卡表、实体/Mapper/Service/Controller/API 和前端卡片管理 UI。
8. 实现主题/语言偏好 localStorage 与基础 UI 切换。
9. 运行前端构建和后端测试；重点烟测登录用户导航、个人中心五板块、设置五模块、手机号验证码、密码修改、银行卡增删。

Rollback strategy: 前端可恢复 `App.vue` 导航配置并移除 `/settings` 路由；后端新增接口不影响现有流程。若银行卡迁移已执行，保留空表不影响现有支付流程，必要时通过单独 SQL 回滚表。

## Open Questions

- 银行卡号在课程项目中是否需要真正加密存储，还是只做脱敏展示与测试数据管理。
- 语言切换是否只覆盖导航/设置页，还是要求全站完整国际化。
- 订单模块中的第五个状态摘要是否最终命名为“待支付”更符合当前业务，还是按产品文案另定。
