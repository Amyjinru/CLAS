## Why

商家端当前把接单管理、经营分析、商品管理、团购管理、客户消息等入口放在左侧模块导航中，并把“信息修改”也混在同一入口区，导致店铺基本信息和工作模块切换耦合。需要将商家端入口重构为更清晰的“商家工作台 / 商家信息”顶部分类，并确保进入各工作模块时店铺基本信息保持在固定位置，减少页面跳转带来的视觉位移。

## What Changes

- 商家端顶部分类 SHALL 调整为两个一级入口：`商家工作台` 和 `商家信息`。
- `商家信息` SHALL 取代当前左侧“信息修改”入口，用于查看和修改店铺资料。
- `商家工作台` SHALL 承载接单管理、经营分析、商品管理、团购管理、客户消息等工作模块。
- 当商家在接单管理、经营分析、商品管理、团购管理、客户消息之间切换时，店铺基本信息 SHALL 保持在原处不动，工作模块内容在固定信息区旁或下方切换。
- 商家端导航按钮与模块排布 SHALL 保持整齐、对齐，并沿用既有商家端视觉风格。
- 不改变现有商家业务 API；本变更主要是前端布局、路由和组件组织调整。

## Capabilities

### New Capabilities
- `merchant-workspace-navigation`: 商家端工作台/商家信息一级导航、固定店铺基本信息展示、以及工作模块切换行为。

### Modified Capabilities
- `ui-design-polish`: 商家端顶部分类、模块按钮和固定店铺信息区需要遵循现有 UI 对齐、间距、按钮状态和响应式排布规范。

## Impact

- Affected frontend views/components: `MerchantConsoleView.vue`, `MerchantAnalyticsView.vue`, `MerchantProductsView.vue`, `MerchantDealsView.vue`, `MerchantMessagesView.vue`, `MerchantSidebar.vue`, `MerchantProfileEditDialog.vue`, and potentially a new shared merchant layout component.
- Affected routing: merchant routes may be reorganized under a shared merchant workspace shell while preserving existing URLs or adding redirects.
- Affected UX: merchant module navigation, shop profile entrypoint, fixed shop information panel, responsive layout.
- No backend API or database changes are expected.
