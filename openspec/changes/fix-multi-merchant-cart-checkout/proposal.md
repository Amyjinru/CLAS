## Why

The current cart submits every item from the first merchant, provides no selection controls, and cannot represent a cross-merchant checkout. Paid orders are also omitted from the user-facing receiving category until they reach a narrow delivery state, so the personal center does not reflect the actual post-payment workflow.

## What Changes

- Group cart items by merchant and add item-level and tri-state merchant selection, initially unchecked.
- Preview and total only selected valid items, with one coupon selection per selected merchant.
- Create one order per selected merchant in a single transaction and route directly to an aggregate payment page.
- Add aggregate payment status and payment submission while preserving existing single-order APIs.
- Classify paid, not-yet-delivered orders under “待收货/使用” consistently in the order list and personal-center count.

## Capabilities

### New Capabilities
- `multi-merchant-checkout`: Selected-item cart preview, atomic multi-merchant order creation, and aggregate payment behavior.
- `user-order-receiving-classification`: Consistent classification and counting of paid orders that have not been delivered.

### Modified Capabilities
- `transaction-consistency`: Multi-order creation must be atomic and aggregate payment retries must not duplicate stock or coupon mutations.

## Impact

- Backend cart, order, and payment DTOs, controllers, and services.
- Frontend cart, payment, order list, personal center, API wrappers, and router.
- Integration tests for selected-item checkout, rollback, aggregate payment, and status filtering.
- No new third-party dependencies or required database migration.
