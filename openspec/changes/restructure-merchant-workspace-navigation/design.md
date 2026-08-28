## Context

商家端目前由多个独立页面组成：`MerchantConsoleView.vue` 承载接单管理和店铺信息，`MerchantAnalyticsView.vue`、`MerchantProductsView.vue`、`MerchantDealsView.vue`、`MerchantMessagesView.vue` 等页面各自引入 `MerchantSidebar` 和资料弹窗。左侧导航中包含工作模块入口和“信息修改”入口，导致店铺基本信息、导航和模块内容分散维护。

本变更要求将商家端一级入口重构为“商家工作台 / 商家信息”，其中“商家信息”取代左侧“信息修改”入口；当进入接单管理、经营分析、商品管理、团购管理、客户消息时，店铺基本信息区域保持在固定位置不动，仅工作模块内容切换。

## Goals / Non-Goals

**Goals:**
- 建立共享的商家端 shell/layout，用于承载顶部一级分类、固定店铺基本信息区域、工作模块导航和内容插槽。
- 将“信息修改”从左侧工作模块导航中移除，改由“商家信息”一级入口进入。
- 工作模块切换时复用同一店铺基本信息组件，避免各页面重复实现或视觉位置漂移。
- 保留接单管理、经营分析、商品管理、团购管理、客户消息的现有业务能力和可访问入口。
- 保持现有路由可用，必要时通过 redirect 或 shared wrapper 平滑迁移。

**Non-Goals:**
- 不改变商家订单、商品、团购、消息、统计 API。
- 不改变商家资料编辑弹窗内字段和验证码规则。
- 不重做管理员端或用户端导航。
- 不新增数据库字段。

## Decisions

1. Introduce a shared merchant workspace shell.
   - 新增或改造一个共享组件承载顶部一级分类、店铺基本信息、工作模块导航和 `<RouterView>`/slot。
   - 这样店铺基本信息只加载和渲染一次，模块切换时内容区域变化，固定信息区保持位置稳定。
   - Alternative considered: 在每个商家页面复制顶部分类和店铺信息。拒绝该方案，因为重复实现会导致后续样式、数据刷新和入口状态不一致。

2. Keep existing merchant module URLs stable.
   - `/merchant-console`、`/merchant/analytics`、`/merchant/products`、`/merchant/deals`、`/merchant/messages` 应继续可访问。
   - 实现时可以通过共享 layout 包裹这些页面，或先抽取组件再逐步迁移路由。
   - Alternative considered: 只新增一个 `/merchant/workspace` 单页并废弃旧 URL。拒绝该方案，因为会破坏已有跳转和书签。

3. Replace the sidebar edit action with a top-level Merchant Info entry.
   - `MerchantSidebar` 不再展示“信息修改”；顶部分类的“商家信息”进入固定资料展示/编辑区域。
   - 商家信息页面可以展示完整店铺资料，并提供编辑按钮打开 `MerchantProfileEditDialog`。
   - Alternative considered: 同时保留左侧“信息修改”和顶部“商家信息”。拒绝该方案，因为会形成两个入口，和用户目标相冲突。

4. Treat work modules as secondary navigation under 商家工作台.
   - 接单管理、经营分析、商品管理、团购管理、客户消息仍是工作台内部模块。
   - 模块按钮应作为工作台内导航而不是一级分类，避免上方分类过载。

## Risks / Trade-offs

- Route migration may duplicate data loading temporarily -> Keep data fetching in the shared shell for merchant profile and leave module-specific data in each module until fully migrated.
- Existing pages may have local profile dialog state -> Remove or centralize duplicate profile dialog entrypoints during implementation.
- Fixed shop information could consume too much vertical space on small screens -> Use responsive stacking and keep information compact on mobile.
- Legacy links may bypass the shell -> Verify every merchant route renders with the shared shell or redirects into it.
