## Context

商家端资料修改入口集中在 `MerchantProfileEditDialog.vue`，当前已经处理注册绑定手机号、商家联系电话、银行卡号、基础资料和头像上传。用户端账号安全模块在 `UserSettingsView.vue` 中提供手机号变更的验证码发送、提交、会话刷新和错误反馈模式。商家端应复用这类交互与 API 语义，避免敏感字段修改逻辑在多个入口里表现不一致。

本变更主要影响前端组件和既有 API 调用；后端已有用户绑定手机号、商家联系电话验证码、商家银行卡验证码和商家资料更新接口，预计无需新增数据库字段。

## Goals / Non-Goals

**Goals:**
- 商家注册绑定手机号修改复用用户端账号安全的手机号变更流程，成功后刷新当前 session user 和 token。
- 商家联系电话、银行卡号等敏感商家资料仅在字段实际变化时要求验证码，并将验证码随资料更新 payload 提交。
- 验证码发送状态、冷却倒计时、字段变更后清空验证码、提交前校验和错误提示保持一致。
- 商家资料弹窗中的上传、验证码、取消、保存按钮复用用户端按钮风格，尺寸稳定、对齐整齐，并在移动端自然换行。

**Non-Goals:**
- 不新增商家资料字段或数据库表。
- 不重做用户端设置页结构。
- 不改变商家注册、登录、支付、订单等无关流程。
- 不引入新的 UI 框架或全局样式体系。

## Decisions

1. Reuse the user account security API path for account-bound phone changes.
   - 商家账号绑定手机号属于登录账号信息，应继续使用 `sendPhoneChangeCode` 和 `updateBoundPhone`。
   - 成功后调用 `setSessionUser` 写入新用户和 token，避免页面仍持有旧手机号或旧 token。
   - Alternative considered: 将账号手机号变更塞进商家资料更新接口。拒绝该方案，因为账号安全语义已经由用户端接口承载，复用可以减少权限和 session 刷新的重复实现。

2. Keep merchant contact phone and bank verification inside the merchant profile update flow.
   - 联系电话和银行卡号是商家资料字段，应继续使用 `sendMerchantPhoneCode`、`sendMerchantBankCode` 和 `updateMyMerchantProfile`。
   - 只有字段值相对原始商家资料变化时才要求验证码；基础资料变化不应被敏感字段验证码阻塞。
   - Alternative considered: 每次保存都要求所有验证码。拒绝该方案，因为它会让只改店铺名称或地址的常见操作变得不必要地繁琐。

3. Normalize code-field state with a shared local helper or composable pattern.
   - 每个验证码字段都需要相同的状态：`sending`、`cooldown`、`sent`、`lastSentValue`、`code` 重置。
   - 实现时可在组件内提取轻量 helper；若用户端后续也要复用，可再提升为共享 composable。
   - Alternative considered: 保留三套手写倒计时逻辑。拒绝该方案，因为容易出现某一字段漏清空验证码或冷却状态不一致。

4. Use stable layout primitives for button alignment.
   - 验证码行使用同一 class 控制输入框和按钮的网格列宽，按钮设置最小宽度，底部操作区使用统一 gap 和右对齐。
   - 移动端改为单列排列，避免按钮挤压输入框或文本溢出。
   - Alternative considered: 仅调整单个按钮 type。拒绝该方案，因为用户要求的是按钮风格复用和排列整齐，布局稳定性同样是验收标准。

## Risks / Trade-offs

- Account phone update succeeds but merchant profile update fails → Keep success/error messages explicit and reload merchant data after failure so session and merchant profile are not visually conflated.
- Backend code endpoints return different error shapes → Use the project’s existing error message helper if available, otherwise preserve readable fallback messages.
- Countdown timers leak after dialog close → Clear all timers on close/reset and unmount.
- Button style changes affect narrow screens → Verify dialog at desktop and mobile widths, ensuring inputs and buttons do not overlap or overflow.
