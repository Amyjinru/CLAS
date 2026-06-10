## 1. Address Location Synchronization

- [x] 1.1 Update `LocationSelector.vue` so successful automatic location emits updated model state and refreshes the visible delivery address preview.
- [x] 1.2 Ensure auto-located province, city, district, street, full address, longitude, and latitude use the same payload shape as manual location selection.
- [x] 1.3 Preserve an existing selected address when automatic location fails, and show a manual-selection fallback message.
- [x] 1.4 Verify manual province/city/district selection and street editing still update the full address correctly.

## 2. Required Address Validation

- [x] 2.1 Add required validation for contact name, contact phone, delivery address, longitude, and latitude in `ProfileView.vue` before address create/update calls.
- [x] 2.2 Add visible Element Plus form required indicators or rules for 联系人, 联系电话, and 收货位置.
- [x] 2.3 Update `LocationSelector.vue` save-address validation so embedded address saving also requires contact name, contact phone, and complete delivery address.
- [x] 2.4 Use coordinate validation that accepts valid numeric zero values and rejects only missing/null/undefined coordinates.
- [x] 2.5 Optionally add minimal backend validation if address create/update APIs currently accept blank contact name, blank phone, or blank address.

## 3. User Portal Navigation

- [x] 3.1 Define a user portal navigation model with entries for 外卖, 团购, 预订/到店, 消息, and 我的 using existing routes where possible.
- [x] 3.2 Update user-facing navigation UI so authenticated USER accounts can clearly access 外卖, 团购, and 我的 from primary navigation.
- [x] 3.3 Keep existing route guards and role restrictions intact for user, merchant, and admin accounts.
- [x] 3.4 Ensure existing routes for home, deals, bookings, cart, orders, profile, merchant detail, payment, review, and announcements remain reachable.

## 4. Personal Center Information Architecture

- [x] 4.1 Rework `ProfileView.vue` into clear task groups or tabs for 我的交易, 我的购物, 我的券包, 地址与资料, and 消息与服务.
- [x] 4.2 Add visible shortcuts or cards for orders and cart in the personal center without changing their business logic.
- [x] 4.3 Keep existing address management, favorites, group-deal vouchers, and notifications accessible under the new grouping.
- [x] 4.4 Label the coupon/voucher area as 券包 or 优惠券/团购券 to avoid implying unsupported coupon features are fully implemented.
- [x] 4.5 Check desktop and mobile layouts so tabs, cards, and form controls do not overlap or become hard to scan.

## 5. Verification

- [x] 5.1 Run the frontend build or lint command available in the project.
- [x] 5.2 Smoke test automatic location success, automatic location failure, manual address selection, and saved-address submission validation.
- [x] 5.3 Smoke test user navigation links for 外卖, 团购, 预订/到店, 消息, 我的, orders, and cart.
- [x] 5.4 If backend validation is changed, run backend tests or a Maven package/test command.
