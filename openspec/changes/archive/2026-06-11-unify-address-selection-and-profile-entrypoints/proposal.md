## Why

The current address selection behavior is still confusing because automatic location and manual selection can feel like separate address states. Users need two clear ways to choose an address, but every address-consuming page should receive exactly one final address object.

The personal center also groups important resources such as addresses, favorite merchants, vouchers, and notifications, but summary blocks need to be directly clickable so users can jump into the matching section quickly.

## What Changes

- Refine address selection into two explicit input modes:
  - automatic location
  - manual province/city/district/street selection
- Merge both modes into one final selected address object with one contract:
  - province
  - city
  - district
  - street/detail
  - full address
  - longitude
  - latitude
  - source (`auto` or `manual`)
- Apply the unified address selector behavior everywhere the app asks users to choose an address/location:
  - home current-location dialog
  - merchant detail delivery-location dialog
  - user profile address form
  - merchant registration address form
  - any embedded save-address flow in the selector
- Sort manual province/city/district wheel options by pinyin initial, so users can scan administrative divisions predictably.
- Keep one final address preview after either mode is used.
- Make personal-center summary blocks clickable:
  - 收货地址 opens/focuses the address section
  - 收藏店铺 opens/focuses favorites
  - 券包 opens/focuses vouchers
  - 未读通知 opens/focuses messages/notifications
- Prefer frontend-only changes unless an existing API contract blocks the behavior.
- No required database schema changes.

## Capabilities

### New Capabilities

- `unified-address-selector`: Defines the two-mode address selector, one final address contract, pinyin-sorted manual wheels, and required usage across address-selection surfaces.
- `profile-entrypoint-cards`: Defines clickable personal-center summary cards that route/focus users into the correct personal-center section.

### Modified Capabilities

- None.

## Impact

- Frontend:
  - `frontend/src/components/LocationSelector.vue`
  - `frontend/src/views/HomeView.vue`
  - `frontend/src/views/MerchantDetailView.vue`
  - `frontend/src/views/ProfileView.vue`
  - `frontend/src/views/MerchantRegisterView.vue`
  - possibly `frontend/src/components/AmapLocationPicker.vue` if it remains an independent address selection surface
- Backend:
  - no required changes
- Database:
  - no required schema changes
- Conflict boundary:
  - avoid changing order/payment/refund business logic
  - avoid changing merchant/admin workflows except merchant registration address selection
  - build on the completed `enhance-user-address-and-navigation` behavior rather than reverting it
