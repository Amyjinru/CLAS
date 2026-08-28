## Why

在上一个迭代（enhance-user-center-settings-navigation）完成后，发现 4 个影响用户体验的具体问题需要修复：

1. **个人中心五个模块横向布局**：`ProfileSummary.vue` 中 5 个统计卡片使用 4 列网格，导致第 5 个卡片换行，视觉上不整齐。
2. **「待收货/使用」过滤逻辑不精确**：当前过滤条件过于宽泛（包含 `PAID` 和 `ACCEPTED` 全部状态），未区分「已配送待收货」和「已购买待使用」的团购券，且没有排除退款/取消订单。
3. **商家端商品管理按钮对齐**：搜索栏中 `margin-right`/`margin-left` 与 flex `gap` 混用导致间距不一致，且缺少 `align-items: center` 导致垂直方向可能偏移。
4. **商家手机号/银行卡验证码功能不可用**：`MerchantProfileEditDialog.vue` 中发送验证码按钮的 `:disabled` 条件要求手机号与银行卡号**同时**合法（`sensitiveValid = phoneValid && bankValid`），导致任一字段为空时两个按钮均被禁用，用户点击无反应且验证码输入框永不显示。

## What Changes

### 1. ProfileSummary 五列布局
- 将 `grid-template-columns` 从 `repeat(4, minmax(0, 1fr))` 改为 `repeat(5, minmax(0, 1fr))`
- 调整响应式断点：中等屏幕（≤1200px）保持 5 列但缩小间距，小屏幕（≤900px）变 3 列，手机端（≤640px）变 1 列

### 2. 「待收货/使用」订单过滤逻辑修正
- 外卖订单：仅包含 `ACCEPTED` 且 `deliveryStatus` 为 `DELIVERING` 或 `DELIVERED`（已配送但未确认收货）
- 团购券：包含 `DealOrder` 中 `status = 'UNUSED'` 的条目（已购买但未使用）
- 明确排除：`REFUNDED`、`REFUND_PENDING`、`CANCELED`、`REJECTED` 状态的订单
- 修复 `status.js` 中 `REFUND_REQUESTED` → `REFUND_PENDING` 的键名不一致问题

### 3. 商家商品管理搜索栏对齐
- 移除搜索栏内联 `margin-right`/`margin-left`，统一由 `.search-bar` 的 `gap: 10px` 控制间距
- 添加 `align-items: center` 确保垂直居中对齐

### 4. 商家验证码发送逻辑修复
- 将共享的 `sensitiveValid` 拆分为各字段独立的发送条件：手机号按钮仅需 `phoneChanged && phoneValid`，银行卡按钮仅需 `bankChanged && bankValid`
- `sendCode()` 函数改为接收参数区分触发源，仅校验对应字段
- 保留共享验证码输入框（因为后端使用同一个 `merchant-profile` 场景），但从任一按钮发送后均可显示

## Capabilities

### Modified Capabilities

- `user-center-dashboard`: ProfileSummary 网格布局从 4 列改为 5 列
- `user-portal-navigation`: OrdersView 待收货/使用 tab 过滤逻辑修正

### New Capabilities

- 无

## Impact

- Frontend:
  - `frontend/src/components/profile/ProfileSummary.vue` — CSS 网格列数变更
  - `frontend/src/views/OrdersView.vue` — 待收货/使用过滤逻辑修正，纳入 DealOrder
  - `frontend/src/utils/status.js` — REFUND_REQUESTED 键名修正
  - `frontend/src/views/MerchantProductsView.vue` — 搜索栏 CSS 对齐
  - `frontend/src/components/merchant/MerchantProfileEditDialog.vue` — 验证码按钮条件拆分
- Backend:
  - 无需修改
- Database:
  - 无需修改
- Dependencies:
  - 无新增依赖
