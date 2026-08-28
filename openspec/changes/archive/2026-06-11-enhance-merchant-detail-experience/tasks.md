## 1. Page State and Loading

- [x] 1.1 Add page-level `loading`, `loadError`, and `favoriteLoading` state to `MerchantDetailView.vue`.
- [x] 1.2 Show a loading state while merchant detail, product list, cart, and favorite state are being fetched.
- [x] 1.3 Show a non-crashing error state if merchant detail loading fails.
- [x] 1.4 Keep existing route watch behavior working after state changes.

## 2. Merchant Header Experience

- [x] 2.1 Redesign the merchant header section to show name, category, score, address, business hours, average price, min order price, delivery fee, and delivery radius.
- [x] 2.2 Make favorite state visually clear and disable the favorite button while the operation is in progress.
- [x] 2.3 Show useful fallback text for missing merchant fields.
- [x] 2.4 Keep the back button and route target unchanged.

## 3. Delivery Context

- [x] 3.1 Improve current location and delivery estimate display near the merchant header or map area.
- [x] 3.2 Show distance or route distance, estimated minutes, and delivery availability when available.
- [x] 3.3 Show a clear prompt to choose location when no usable location exists.
- [x] 3.4 Keep `MerchantRouteMap` integration working and preserve manual location selection.
- [x] 3.5 Keep delivery estimate failure non-blocking.

## 4. Product List Experience

- [x] 4.1 Improve product cards/rows to show product name, description, price, stock, and sold-out state.
- [x] 4.2 Add image or visual placeholder support using existing `product.image` when available.
- [x] 4.3 Disable add button for sold-out products and make the sold-out state visually clear.
- [x] 4.4 Add an empty product state when the merchant has no available products.
- [x] 4.5 Keep add-to-cart behavior and cart drawer behavior unchanged.

## 5. Operation Feedback

- [x] 5.1 Show success/failure feedback for favorite and unfavorite operations.
- [x] 5.2 Keep add-to-cart success/failure feedback visible.
- [x] 5.3 Avoid stale favorite state by refreshing favorites after the operation.
- [x] 5.4 Ensure message text does not overlap with merchant/product content.

## 6. Verification

- [x] 6.1 Test normal merchant detail loading with products.
- [x] 6.2 Test sold-out product button state.
- [x] 6.3 Test favorite and unfavorite flow.
- [x] 6.4 Test page behavior with no current location.
- [x] 6.5 Test manual location selection still updates delivery estimate.
- [x] 6.6 Test empty product state if possible using a merchant with no available products or mocked data.
- [x] 6.7 Run frontend build after implementation.

## 7. Conflict Avoidance

- [x] 7.1 Do not edit cart business rules, order creation, payment, merchant console, admin pages, or database schema files.
- [x] 7.2 Coordinate before modifying shared router, shared API client, global CSS, or schema files.
- [x] 7.3 Keep the final change focused on `MerchantDetailView.vue` unless a small API compatibility issue is discovered.
