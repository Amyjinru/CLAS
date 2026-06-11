## Why

当前用户端已经有外卖、团购、预约、购物车、订单、个人中心、地址、收藏、通知、资料编辑等基础能力，但顶部入口和个人中心信息结构仍偏散，用户需要在多个页面间猜测功能位置。此次变更将用户端导航、个人中心和设置页重组为更贴近日常生活服务平台的结构，让订单、购物、券包、消息、账号安全和支付管理都有明确入口。

## What Changes

- 将普通用户顶部大分类调整为：外卖、团购、预订/到店、个人中心、设置。
- 保留并复用现有 `/home`、`/deals`、`/bookings`、`/profile` 等路由，新增或补齐 `/settings` 用户设置入口。
- 将个人中心重组为五个大板块：订单、购物车、收藏、券包、消息。
- 在个人中心的订单板块中展示五个订单模块入口：全部订单、待收货/使用、待评价、退款/售后，以及与“全部订单”同级的状态摘要入口；具体列表优先复用 `OrdersView.vue`，必要时通过 query/tab 过滤。
- 将设置页拆分为五个大模块：个人信息、收货地址、账号安全、支付设置、通用设置。
- 个人信息复用现有 `getProfile`、`updateProfile`、`uploadAvatar`，支持头像和昵称修改。
- 收货地址复用现有地址组件和地址 API，支持修改位置、联系人、联系电话。
- 账号安全支持绑定手机号修改验证码流程，以及密码修改流程：输入当前密码，新密码二次确认。
- 支付设置新增银行卡管理能力，支持绑定多张银行卡、展示脱敏卡号、删除银行卡；交互参考主流支付网站的“卡片列表 + 添加卡片弹窗 + 删除确认”模式。
- 通用设置支持语言选择和白天/黑夜模式切换，并与现有前端主题风格保持一致。

## Capabilities

### New Capabilities

- `user-portal-navigation`: Defines the ordinary user top-level navigation categories and routing expectations for 外卖、团购、预订/到店、个人中心、设置.
- `user-center-dashboard`: Defines the user profile center blocks for orders, cart, favorites, vouchers, and messages, including order status module entrypoints.
- `user-settings-center`: Defines user settings modules for profile info, delivery addresses, account security, payment cards, language, and light/dark mode.

### Modified Capabilities

- None.

## Impact

- Frontend:
  - `frontend/src/App.vue` for ordinary user top navigation.
  - `frontend/src/router/index.js` for `/settings` and optional order query/tab route support.
  - `frontend/src/views/ProfileView.vue` and existing profile components for the personal center block layout.
  - New or adjusted `frontend/src/views/UserSettingsView.vue` and optional settings components under `frontend/src/components/profile/` or `frontend/src/components/settings/`.
  - Existing API modules: `frontend/src/api/profile.js`, `frontend/src/api/address.js`, `frontend/src/api/order.js`, `frontend/src/api/cart.js`, `frontend/src/api/favorite.js`, `frontend/src/api/coupon.js`, `frontend/src/api/notification.js`.
  - New or extended API module for bank cards, likely `frontend/src/api/payment.js`.
- Backend:
  - Reuse `UserProfileController`, `AddressController`, existing notification/favorite/cart/order/coupon controllers where possible.
  - Add password-change endpoint to the user profile/account surface if not already available.
  - Add bank-card CRUD endpoints and service methods with owner checks and masked responses.
- Database:
  - Add a user bank card table if no reusable payment-card table exists.
  - Optional lightweight user preference persistence for language/theme; if deferred, store preferences in localStorage first and document the limitation.
- Dependencies:
  - No required new frontend UI framework; continue using Vue 3, Element Plus, existing CSS variables, and current API client patterns.
