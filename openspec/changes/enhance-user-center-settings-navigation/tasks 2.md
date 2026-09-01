## 1. User Portal Navigation

- [x] 1.1 Update ordinary user navigation in `frontend/src/App.vue` to show 外卖、团购、预订/到店、个人中心、设置 in order.
- [x] 1.2 Add `/settings` route in `frontend/src/router/index.js` with `roles: ['USER']`, `userPortal: true`, and a clear page title.
- [x] 1.3 Remove 购物车、订单、消息、商家入驻 from ordinary user top-level nav while preserving merchant, admin, and guest nav behavior.
- [x] 1.4 Verify active navigation styling works for `/home`, `/deals`, `/bookings`, `/profile`, and `/settings`.

## 2. Personal Center Dashboard

- [x] 2.1 Refactor `frontend/src/views/ProfileView.vue` so the primary personal-center sections are 订单、购物车、收藏、券包、消息.
- [x] 2.2 Add an order block with entrypoints for 全部订单、待支付、待收货/使用、待评价、退款/售后 using existing order status and review data.
- [x] 2.3 Add a cart block that summarizes cart/pending payment state and routes users to `/cart`.
- [x] 2.4 Keep favorites management in the personal center using existing `listFavorites` and `removeFavorite` APIs.
- [x] 2.5 Keep 券包 in the personal center using existing group-deal voucher data and include claimed coupon data when existing coupon API support is available.
- [x] 2.6 Keep the message block using existing notification APIs, unread count, mark-read actions, clear/read-all actions where already supported, and platform announcement entrypoint.
- [x] 2.7 Split new personal-center blocks into focused components under `frontend/src/components/profile/` if `ProfileView.vue` becomes too large or mixes unrelated workflows.

## 3. User Settings Frontend

- [x] 3.1 Create `frontend/src/views/UserSettingsView.vue` using existing `user-page`, `panel`, Element Plus form/tabs/dialog patterns, and project CSS variables.
- [x] 3.2 Implement the 个人信息 module by reusing existing profile APIs for avatar upload and nickname update.
- [x] 3.3 Implement the 收货地址 module by reusing `ProfileAddressSection` or the same address API/component behavior for location, contact person, and contact phone.
- [x] 3.4 Implement the 账号安全 module with phone-change verification code form and password-change form with current password, new password, and confirmation validation.
- [x] 3.5 Implement the 支付设置 module with a masked bank-card list, add-card dialog, delete confirmation, loading states, and empty state.
- [x] 3.6 Implement the 通用设置 module with language selector and light/dark mode switch persisted in localStorage.
- [x] 3.7 Ensure settings page layout is responsive and visually consistent with existing user pages on desktop and mobile.

## 4. Account Security Backend

- [x] 4.1 Add `PasswordChangeRequest` DTO with current password, new password, and confirm password validation fields.
- [x] 4.2 Add `PUT /api/user/password` to the user profile/account controller surface with `@RequireRole({"USER", "MERCHANT", "ADMIN"})` or the selected role scope.
- [x] 4.3 Implement password-change service logic that verifies current password, validates new password strength, checks confirmation match, encodes the new password, and updates only the current user.
- [x] 4.4 Add frontend API helper in `frontend/src/api/profile.js` for password change.
- [x] 4.5 Add focused backend tests for successful password change, wrong current password, and mismatched confirmation.

## 5. Bank Card Management Backend

- [x] 5.1 Add database migration for `user_bank_card` with user owner, bank name, cardholder name, masked/last4 card fields, card type, default flag, and create time.
- [x] 5.2 Add `UserBankCard` entity, mapper, request DTO, and response DTO that never returns the full card number.
- [x] 5.3 Add `UserBankCardService` with list, create, and delete methods scoped to `UserContext.getUserId()`.
- [x] 5.4 Add controller endpoints `GET /api/user/bank-cards`, `POST /api/user/bank-cards`, and `DELETE /api/user/bank-cards/{id}`.
- [x] 5.5 Add frontend API helpers in `frontend/src/api/payment.js` or a new `bankCard.js` exported by `clas.js`.
- [x] 5.6 Add backend tests for multiple card binding, masked response, delete ownership, and no full card number exposure.

## 6. Preferences And Theme

- [x] 6.1 Add a small frontend preference utility or composable for `clas-theme-mode` and `clas-language`.
- [x] 6.2 Extend `frontend/src/styles/theme.css` with dark-mode variables while keeping existing light theme unchanged.
- [x] 6.3 Apply selected theme on app startup and when the user toggles the mode in settings.
- [x] 6.4 Apply selected language to top navigation and settings labels that are part of this change.

## 7. Verification

- [x] 7.1 Run frontend build from `frontend/` and fix any compile or lint errors.
- [x] 7.2 Run backend tests with Maven and fix regressions.
- [ ] 7.3 Smoke test ordinary user flows: login, top navigation, `/profile` five blocks, `/settings` five modules, phone code request, password change validation, bank-card add/delete, theme toggle.
- [x] 7.4 Confirm merchant, admin, and guest navigation remain unchanged.
- [x] 7.5 Review CSS for consistency with existing variables and avoid new hardcoded one-off palettes.

Note: Task 7.3 is implemented and covered by build/API tests where possible, but interactive browser smoke testing is blocked in this environment because the in-app browser is unavailable and Playwright is not installed locally.
