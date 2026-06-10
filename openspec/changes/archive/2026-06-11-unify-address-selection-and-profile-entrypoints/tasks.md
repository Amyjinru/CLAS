## 1. Unified Address Selector Contract

- [x] 1.1 Define a normalized selected-location object in `LocationSelector.vue` with province, city, district, street, address, longitude, latitude, and source.
- [x] 1.2 Refactor `LocationSelector.vue` so automatic location and manual selection are two clear modes but both write to one final selected-location state.
- [x] 1.3 Replace separate parent-facing auto/manual event handling with one final address update/confirm path, while keeping short-term compatibility only if needed.
- [x] 1.4 Ensure switching modes replaces the final address only after the new mode has a valid address and coordinates.
- [x] 1.5 Preserve the previous final address when automatic location or manual geocoding fails.

## 2. Manual Wheel Sorting

- [x] 2.1 Add a local helper to sort province, city, and district options by Chinese pinyin-friendly order.
- [x] 2.2 Apply the sorter to province loading results.
- [x] 2.3 Apply the sorter to city loading results after province selection.
- [x] 2.4 Apply the sorter to district loading results after city selection.
- [x] 2.5 Spot check province/city/district wheel ordering in the browser or with a small deterministic fixture.

## 3. Address Selection Usage Coverage

- [x] 3.1 Update `HomeView.vue` location dialog to consume the unified selected address object.
- [x] 3.2 Update `MerchantDetailView.vue` delivery-location dialog to consume the unified selected address object.
- [x] 3.3 Update `ProfileView.vue` saved-address form to consume the unified selected address object.
- [x] 3.4 Update `MerchantRegisterView.vue` address form to consume the unified selected address object.
- [x] 3.5 Review `AmapLocationPicker.vue` usage and either align its emitted payload shape or document why it is outside this address-selector flow.

## 4. Personal Center Clickable Entrypoints

- [x] 4.1 Add target tab metadata to personal-center summary cards for 收货地址, 收藏店铺, 券包, and 未读通知.
- [x] 4.2 Render summary cards as keyboard-accessible clickable controls.
- [x] 4.3 Make clicking 收货地址 switch to 地址与资料.
- [x] 4.4 Make clicking 收藏店铺 switch to 我的购物.
- [x] 4.5 Make clicking 券包 switch to 我的券包.
- [x] 4.6 Make clicking 未读通知 switch to 消息与服务.
- [x] 4.7 Add hover/focus/active styling so clickable cards feel interactive without disrupting the current layout.

## 5. Verification

- [x] 5.1 Run the frontend build.
- [x] 5.2 Smoke test automatic location and manual selection in profile address form.
- [x] 5.3 Smoke test address selection from home, merchant detail, and merchant registration.
- [x] 5.4 Smoke test pinyin-friendly wheel ordering for province, city, and district lists.
- [x] 5.5 Smoke test personal-center summary cards with pointer and keyboard activation.
