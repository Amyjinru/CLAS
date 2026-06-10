## Why

The user profile center is the main place where ordinary users manage delivery addresses, favorites, deal vouchers, and notifications, but the current page is still a basic list-and-form view with limited state feedback and incomplete management actions.

This change gives Member A a focused user-side enhancement package for profile-center usability while avoiding transaction, merchant-console, admin, and database-schema work.

## What Changes

- Improve the user profile center layout and information hierarchy.
- Add loading, empty, and operation feedback states for addresses, favorites, deal vouchers, and notifications.
- Improve address management with clearer form state, default-address feedback, map-selected location preview, and optional edit support.
- Improve favorites with direct merchant navigation and optional remove-from-favorites action.
- Improve notification center with unread count, read status, and optional mark-all-read support.
- Keep existing address, favorite, notification, and deal-order APIs where possible.
- No required database schema changes.

## Capabilities

### New Capabilities

- `user-profile-center`: Enhances the user-facing profile center for address management, favorites, deal vouchers, notifications, and feedback states.

### Modified Capabilities

- None.

## Impact

- Frontend:
  - `frontend/src/views/ProfileView.vue`
  - possibly `frontend/src/api/address.js`, `frontend/src/api/notification.js`, or `frontend/src/api/favorite.js` for small helper additions
- Backend:
  - optional address update endpoint
  - optional mark-all-notifications-read endpoint
  - only if selected during apply
- Database:
  - no required schema changes
- Conflict boundary:
  - avoid cart, order, payment, merchant console, admin pages, router, global styles, and schema files
