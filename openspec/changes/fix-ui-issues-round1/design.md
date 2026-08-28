## Context

上一个迭代完成后，用户端个人中心和商家端存在 4 个待修复的 UI/UX 问题。当前代码状态：

- `ProfileSummary.vue`：5 个统计卡片，4 列网格 `grid-template-columns: repeat(4, minmax(0, 1fr))`，第 5 个卡片换行
- `OrdersView.vue`：「待收货/使用」tab 过滤条件为 `['PAID', 'ACCEPTED'].includes(status) || ['WAITING', 'PREPARING', 'DELIVERING'].includes(deliveryStatus)`，未处理团购券且范围过宽
- `MerchantProductsView.vue`：搜索栏有 `display: flex; gap: 10px` 但内联样式 `margin-right: 12px` / `margin-left: 12px` 破坏了间距一致性
- `MerchantProfileEditDialog.vue`：`sensitiveValid = phoneValid && bankValid`，两个发送按钮的 `:disabled` 均依赖此条件

## Goals / Non-Goals

**Goals:**
- 5 个统计卡片在桌面端同一横排显示
- 「待收货/使用」精确包含已配送待收货订单和未使用团购券，不含退款/取消
- 商家商品管理搜索栏按钮和下拉框垂直对齐
- 商家修改手机号或银行卡时，任一字段的发送验证码按钮均可独立工作，验证码输入框正常显示
- 复用现有组件、API、CSS 变量，保持代码风格一致

**Non-Goals:**
- 不改动个人中心 tab 结构（订单/购物车/收藏/券包/消息仍为 tabs）
- 不新增后端接口
- 不新增数据库迁移
- 不修改验证码后端发送逻辑（仍为 console 输出）

## Decisions

### Decision 1: ProfileSummary 五列网格 + 渐进式响应断点

将 `ProfileSummary.vue` 的 grid 调整为：

```css
.profile-summary {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(5, minmax(0, 1fr));  /* 4 → 5 */
}

@media (max-width: 1100px) {
  .profile-summary { grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 8px; }
}
@media (max-width: 900px) {
  .profile-summary { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .profile-summary { grid-template-columns: 1fr; }
}
```

备选方案：改为 `repeat(auto-fit, minmax(180px, 1fr))` 自动换行。暂不采用，因为需要精确控制 5 列在同一行，auto-fit 在容器宽度不足时会提前换行。

### Decision 2: 「待收货/使用」重新定义过滤条件

在 `OrdersView.vue` 中：

```js
// 待收货/使用
if (activeTab.value === 'receiving') {
  return orders.value.filter((entry) => {
    const { status, deliveryStatus } = entry.order
    // 明确排除退款和取消
    if (['REFUNDED', 'REFUND_PENDING', 'CANCELED', 'REJECTED'].includes(status)) return false
    if (entry.order.refundStatus && entry.order.refundStatus !== 'NONE') return false
    // 已配送但未确认收货
    if (status === 'ACCEPTED' && ['DELIVERING', 'DELIVERED'].includes(deliveryStatus)) return true
    return false
  })
}
```

同时在 `ProfileOrderBlock.vue` 的父组件（`ProfileView.vue`）中传入的 `modules` 数据里，待收货/使用 count 应包含：
- 外卖订单中符合上述过滤条件的数量
- 团购券中 `status === 'UNUSED'` 的数量（`DealOrder`）

参考主流电商平台（美团、淘宝）的做法：待收货 = 已发货未签收；待使用 = 已购未消费的券/码。

备选方案：在后端新增聚合接口。暂不采用，前端已有订单和团购券数据源。

### Decision 3: 修复 REFUND_PENDING 键名不一致

在 `frontend/src/utils/status.js` 中：

```diff
- REFUND_REQUESTED: { text: '退款中', type: 'warning' },
+ REFUND_PENDING: { text: '退款中', type: 'warning' },
```

同步修改 `OrdersView.vue` 中「退款/售后」tab 的过滤条件：

```diff
- entry.order.status === 'REFUND_REQUESTED' ||
+ entry.order.status === 'REFUND_PENDING' ||
```

备选方案：改后端常量。不采用，因为后端 `REFUND_PENDING` 语义更准确（pending 而非 requested）。

### Decision 4: 搜索栏 CSS 统一

在 `MerchantProductsView.vue` 中：

1. 移除内联 `margin-right: 12px`（搜索输入框）和 `margin-left: 12px`（分类选择器）
2. 在 `.search-bar` 中添加 `align-items: center`
3. 可选：为搜索输入框和分类选择器添加 scoped CSS class 代替内联 `style`

```css
.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;        /* 新增：垂直居中 */
  margin-bottom: 20px;
}

.search-input { width: 300px; }
.category-select { width: 180px; }
```

备选方案：使用 Element Plus 的 `el-row`/`el-col` 栅格。暂不采用，因为当前 flex 布局更轻量且与其他页面风格一致。

### Decision 5: 商家验证码按钮条件拆分

将 `MerchantProfileEditDialog.vue` 中：

1. **移除 `sensitiveValid`** 对按钮 disabled 的绑定
2. **手机号发送按钮** 仅依赖 `phoneChanged && phoneValid`：
   ```html
   :disabled="!phoneChanged || !phoneValid || profileCodeCooldown > 0"
   ```
3. **银行卡发送按钮** 仅依赖 `bankChanged && bankValid`：
   ```html
   :disabled="!bankChanged || !bankValid || profileCodeCooldown > 0"
   ```
4. **`sendCode()` 函数** 接收来源参数，仅校验对应字段（而非同时校验两个字段）：
   ```js
   async function sendCode(source) {
     if (source === 'phone') {
       if (!phoneChanged.value) return
       if (!phoneValid.value) { ElMessage.warning('请输入正确手机号'); return }
     } else if (source === 'bank') {
       if (!bankChanged.value) return
       if (!bankValid.value) { ElMessage.warning('请输入 9 到 25 位银行卡号'); return }
     }
     // ... 发送逻辑不变
   }
   ```
5. 模板中按钮 `@click` 改为 `@click="sendCode('phone')"` 和 `@click="sendCode('bank')"`

**为什么保留共享验证码输入框：** 后端 `sendProfileUpdateCode` 将验证码发送到商家手机号（`merchant-profile` 场景），`updateMyProfile` 在 phone 或 bank 任一变更时校验该验证码。前后端共用一套场景码是合理的设计。用户修改任一敏感字段都需要手机验证码确认，这与主流网站（支付宝、淘宝）的「修改安全信息需短信验证」模式一致。

备选方案：前后端各新增独立验证码场景。暂不采用，会增加不必要的复杂度。

## Risks / Trade-offs

- [Risk] 5 列网格在小屏幕（1100px-1400px）可能卡片内容拥挤 → Mitigation: 在 1100px 断点缩小 gap 和 padding，必要时缩小字号
- [Risk] `REFUND_PENDING` 键名修改可能影响其他引用处 → Mitigation: 全局搜索 `REFUND_REQUESTED` 确保所有引用同步修改
- [Risk] 验证码条件放宽后可能存在不填银行卡就发送验证码的场景 → Mitigation: 后端 `sendProfileUpdateCode` 已做独立校验，前端仅调整按钮可用性

## Migration Plan

1. 修改 `ProfileSummary.vue` CSS 网格
2. 修改 `status.js` 键名 + 全局搜索替换
3. 修改 `OrdersView.vue` 过滤逻辑
4. 修改 `ProfileView.vue` 中待收货计数逻辑（纳入 DealOrder UNUSED）
5. 修改 `MerchantProductsView.vue` 搜索栏 CSS
6. 修改 `MerchantProfileEditDialog.vue` 验证码逻辑
7. 运行前端 build 确认无编译错误
8. 运行后端测试确认无回归

Rollback: 所有改动限定在前端，可通过 `git revert` 回滚。

## Open Questions

- 「待收货/使用」中 `PAID` 状态（已支付待商家接单）是否也应纳入？当前方案仅纳入 `ACCEPTED + DELIVERING/DELIVERED`，与「已配送」的语义一致。如果产品需要更宽泛的定义（包含待接单），可后续调整。
