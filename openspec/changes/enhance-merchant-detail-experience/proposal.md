## Why

The merchant detail page is the main conversion page after home-page discovery, but the current experience is still rough: merchant information is compressed into plain text, delivery and favorite states are not prominent, product descriptions are underused, and empty/loading/error states are limited.

This change gives Member A a focused user-side enhancement package for merchant detail experience without changing cart/order/payment business rules or touching merchant/admin workspaces.

## What Changes

- Improve merchant detail page information hierarchy and visual clarity.
- Highlight merchant category, score, business hours, min order price, delivery fee, delivery radius, current location, and delivery availability.
- Improve favorite button feedback and state visibility.
- Improve product list presentation with description, price, stock/sold-out state, and image fallback where practical.
- Add loading, empty, and error feedback for merchant detail and product loading.
- Keep the existing add-to-cart and submit-order flows intact.
- No breaking API or database changes are planned.

## Capabilities

### New Capabilities

- `merchant-detail-experience`: Improves the user-facing merchant detail page with clearer merchant info, delivery status, product display, and feedback states.

### Modified Capabilities

- None.

## Impact

- Frontend:
  - `frontend/src/views/MerchantDetailView.vue`
  - possibly small local scoped CSS only
- Backend:
  - Usually none required; existing `getMerchant`, `listProducts`, `getDeliveryEstimate`, and favorite APIs are sufficient
- Database:
  - No required schema changes
- Conflict boundary:
  - Avoid cart/order/payment state-machine changes
  - Avoid merchant console, admin pages, router, global CSS, and schema files
