## Why

商家端资料修改目前对需要验证码的信息分散处理，容易与用户端账号安全逻辑产生不一致，也让注册绑定手机号、联系电话、银行卡号等敏感字段的修改校验体验不统一。与此同时，商家端资料弹窗中的验证码按钮和操作按钮排列不够整齐，需要复用用户端既有按钮风格来提升一致性和可维护性。

## What Changes

- 商家端资料修改 SHALL 复用用户端账号安全中“发送验证码、冷却倒计时、验证码提交、变更后刷新登录态”的交互和校验模式。
- 商家端修改注册绑定手机号时 SHALL 使用账号安全的手机号变更流程，并在成功后刷新当前会话用户信息和 token。
- 商家端修改联系电话、银行卡号等需要验证码的敏感商家资料时 SHALL 仅在对应字段发生变化且验证码已发送并填写后允许提交。
- 商家端资料修改界面 SHALL 统一验证码输入、发送按钮、取消/保存按钮的尺寸、状态、对齐方式和排列，复用用户端按钮风格。
- 商家端资料基础信息修改 SHALL 与敏感字段修改解耦，未修改的敏感字段不得要求验证码。

## Capabilities

### New Capabilities
- `merchant-profile-security`: 商家端资料修改中涉及账号绑定手机号、商家联系电话、银行卡号等敏感字段的验证码校验、提交与会话刷新行为。

### Modified Capabilities
- `ui-design-polish`: 商家端资料修改弹窗的按钮样式、按钮状态和表单操作区排列需要遵循用户端已建立的按钮风格与对齐规范。

## Impact

- Affected frontend: `frontend/src/components/merchant/MerchantProfileEditDialog.vue` and shared API helpers used by user account security flows.
- Affected APIs: existing user phone-change code/update APIs and merchant profile phone/bank verification APIs; no new API contract is expected unless implementation finds a missing endpoint.
- Affected UX: merchant profile edit dialog validation, code sending cooldown states, submit gating, session refresh, and button layout.
- No database schema changes are expected.
