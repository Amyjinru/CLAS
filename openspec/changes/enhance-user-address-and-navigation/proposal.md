## Why

The user-side experience currently mixes discovery, delivery address setup, orders, cart, vouchers, and profile management in ways that make common actions harder to find. Address creation also allows incomplete contact details, and automatic location selection does not consistently surface the located address as the delivery address preview.

This change makes the user portal clearer and safer for delivery flows: location results should become visible delivery-address candidates, required delivery fields should be enforced, and user-facing features should be grouped into clearer sections such as delivery, group deals, and personal center.

## What Changes

- Make automatic location behave like manual location selection for delivery address entry:
  - after auto-locate succeeds, show the resolved address in the delivery address field/preview
  - keep province/city/district/street, full address, longitude, and latitude synchronized
  - allow the user to confirm and save the located address without re-selecting manually
- Enforce required delivery address fields before creating or updating an address:
  - contact name is required
  - contact phone is required
  - delivery address/location is required
  - validation feedback should identify the missing field
- Improve the user portal information architecture:
  - separate user-facing entry points into clearer tabs/pages such as 外卖, 团购, and 我的
  - move or surface orders, cart, coupons/vouchers, addresses, favorites, and notifications under a clearer personal-center structure
  - keep quick access to high-frequency tasks from the main user navigation
- Use existing APIs and data models where possible.
- No required database schema changes.

## Capabilities

### New Capabilities

- `user-address-location`: Defines delivery-address location synchronization and required address-field validation.
- `user-portal-navigation`: Defines the user-side navigation structure across delivery, group deals, and personal-center workflows.

### Modified Capabilities

- None.

## Impact

- Frontend:
  - `frontend/src/components/LocationSelector.vue`
  - `frontend/src/views/ProfileView.vue`
  - `frontend/src/views/HomeView.vue`
  - `frontend/src/router/index.js`
  - possibly user portal layout/navigation styles or shared navigation components if introduced
- Backend:
  - no required changes; existing address create/update validation may optionally be tightened if current backend accepts incomplete contact fields
- Database:
  - no required schema changes
- Conflict boundary:
  - avoid changing payment, refund, merchant console, admin workflows, and order state-machine behavior
  - coordinate with existing `enhance-user-profile-center` and `enhance-home-search-filter` changes if they are applied in parallel
