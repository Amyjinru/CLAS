## 1. ProfileSummary 五列布局

- [ ] 1.1 修改 `frontend/src/components/profile/ProfileSummary.vue` 的 `grid-template-columns` 从 `repeat(4, minmax(0, 1fr))` 为 `repeat(5, minmax(0, 1fr))`
- [ ] 1.2 添加 `max-width: 1100px` 断点：保持 5 列但缩小 `gap` 为 8px、`padding` 为 12px
- [ ] 1.3 修改 `max-width: 900px` 断点为 3 列（原为 2 列）
- [ ] 1.4 保留 `max-width: 640px` 单列布局
- [ ] 1.5 验证 1280px 桌面宽度下 5 个卡片同一横排显示

## 2. 订单状态键名修正

- [ ] 2.1 在 `frontend/src/utils/status.js` 中将 `REFUND_REQUESTED` 改为 `REFUND_PENDING`
- [ ] 2.2 全局搜索 `REFUND_REQUESTED`，确认 `OrdersView.vue` 和 `OrderDetailContent.vue` 等文件中的引用同步修改

## 3. 「待收货/使用」过滤逻辑修正

- [ ] 3.1 修改 `frontend/src/views/OrdersView.vue` 中 `receiving` tab 过滤逻辑：
  - 外卖：`ACCEPTED` 且 `deliveryStatus` 为 `DELIVERING` 或 `DELIVERED`
  - 明确排除 `REFUNDED`、`REFUND_PENDING`、`CANCELED`、`REJECTED` 及有退款流程的订单
- [ ] 3.2 同步修正「退款/售后」tab 中的 `REFUND_REQUESTED` → `REFUND_PENDING`
- [ ] 3.3 在 `frontend/src/views/ProfileView.vue` 中，为 `ProfileOrderBlock` 的「待收货/使用」计数纳入 `DealOrder` 中 `status === 'UNUSED'` 的数量
- [ ] 3.4 在 `frontend/src/components/profile/ProfileOrderBlock.vue` 中，如需要，补充「团购待使用」条目的展示（或在计数中说明）

## 4. 商家商品管理搜索栏对齐

- [ ] 4.1 移除 `frontend/src/views/MerchantProductsView.vue` 搜索栏中搜索输入框的 `margin-right: 12px` 内联样式
- [ ] 4.2 移除分类选择器的 `margin-left: 12px` 内联样式
- [ ] 4.3 在 `.search-bar` CSS 中添加 `align-items: center`
- [ ] 4.4 为搜索输入框和分类选择器添加 scoped CSS class（`.search-input`、`.category-select`）替代内联 `style` 宽度

## 5. 商家验证码发送逻辑修复

- [ ] 5.1 修改 `frontend/src/components/merchant/MerchantProfileEditDialog.vue` 中手机号发送按钮的 `:disabled`：从 `!phoneChanged || !sensitiveValid || ...` 改为 `!phoneChanged || !phoneValid || profileCodeCooldown > 0`
- [ ] 5.2 修改银行卡发送按钮的 `:disabled`：从 `!bankChanged || !sensitiveValid || ...` 改为 `!bankChanged || !bankValid || profileCodeCooldown > 0`
- [ ] 5.3 修改 `sendCode()` 函数签名，接收 `source` 参数（`'phone'` 或 `'bank'`），仅校验对应字段而非同时校验两个字段
- [ ] 5.4 更新模板中两个按钮的 `@click` 分别传入 `'phone'` 和 `'bank'`
- [ ] 5.5 移除不再使用的 `sensitiveValid` 计算属性（或保留但不用于按钮 disabled）

## 6. 验证

- [ ] 6.1 运行 `frontend/` build 确认无编译或 lint 错误
- [ ] 6.2 运行后端 Maven 测试确认无回归
- [ ] 6.3 烟测：个人中心 5 统计卡片同排、待收货过滤正确、商家搜索栏对齐、商家验证码可发送
