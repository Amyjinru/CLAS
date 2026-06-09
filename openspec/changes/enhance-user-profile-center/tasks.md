## 1. Page State and Summary

- [x] 1.1 Add `loading`, `loadError`, and operation-state flags to `ProfileView.vue`.
- [x] 1.2 Show a loading state while addresses, deal vouchers, favorites, and notifications load.
- [x] 1.3 Show a non-crashing error state if profile data loading fails.
- [x] 1.4 Add a profile summary section with username, phone, address count, favorite count, voucher count, and unread notification count.

## 2. Address Management UX

- [x] 2.1 Improve the address form layout and show clear map-selected address preview.
- [x] 2.2 Add form reset behavior after successful save and a manual reset/cancel action.
- [x] 2.3 Show operation feedback for saving, setting default, and deleting addresses.
- [x] 2.4 Add empty state when no address exists.
- [x] 2.5 Add delete confirmation before removing an address.
- [x] 2.6 Optional: add address edit mode if backend update endpoint is implemented.

## 3. Favorites and Vouchers UX

- [x] 3.1 Improve favorites list display with merchant name, category, address, and enter-store action.
- [x] 3.2 Optional: add remove-favorite action from profile center.
- [x] 3.3 Add empty state when no favorite merchants exist.
- [x] 3.4 Improve deal voucher display with voucher code, paid amount, and status tag.
- [x] 3.5 Add empty state when no deal vouchers exist.

## 4. Notification Center UX

- [x] 4.1 Show unread notification count in the notification panel.
- [x] 4.2 Visually distinguish unread and read notifications.
- [x] 4.3 Keep single-notification mark-read behavior and refresh data after operation.
- [x] 4.4 Optional: add mark-all-read action if backend endpoint is implemented.
- [x] 4.5 Add empty state when no notifications exist.

## 5. Optional Backend Enhancements

- [x] 5.1 Optional: add `PUT /api/address/{id}` for editing an existing address with ownership checks.
- [x] 5.2 Optional: add `update` method in `AddressService` using `AddressRequest` and coordinate validation.
- [x] 5.3 Optional: add `updateAddress` helper in `frontend/src/api/address.js`.
- [x] 5.4 Optional: add `POST /api/notifications/read-all` for marking current user's notifications as read.
- [x] 5.5 Optional: add `markAllNotificationsRead` helper in `frontend/src/api/notification.js`.

## 6. Verification

- [ ] 6.1 Test profile loading with addresses, favorites, vouchers, and notifications.
- [ ] 6.2 Test empty states for addresses, favorites, vouchers, and notifications where possible.
- [ ] 6.3 Test address save, set-default, and delete flows.
- [ ] 6.4 Test notification single mark-read flow.
- [ ] 6.5 If optional endpoints are implemented, test address edit and mark-all-read flows.
- [x] 6.6 Run frontend build after implementation.
- [x] 6.7 If backend endpoints are implemented, run backend tests.

## 7. Conflict Avoidance

- [x] 7.1 Do not edit cart, order creation, payment, merchant console, admin pages, or database schema files.
- [x] 7.2 Coordinate before modifying shared router, shared API client, global CSS, or schema files.
- [x] 7.3 Keep the final change focused on profile-center management.
