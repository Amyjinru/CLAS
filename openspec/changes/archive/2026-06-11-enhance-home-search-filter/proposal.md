## Why

The CLAS home page is the user's entry point for discovering nearby merchants, but the current search/filter experience is still basic and does not fully expose existing backend capabilities such as deliverable filtering, result feedback, and location-aware sorting.

This change gives Member A a focused, low-conflict feature package to improve home-page merchant discovery without touching transaction, merchant-console, or admin workflows.

## What Changes

- Enhance the user home page merchant search and filtering experience.
- Add visible controls for keyword, category, sort, location/address, and deliverable-only filtering.
- Improve result feedback with loading state, empty state, result count, active filter display, and reset behavior.
- Preserve the existing `/api/merchant/list` endpoint shape and only add backward-compatible parameters if needed.
- Keep the implementation scope mostly within `HomeView.vue`, merchant list API usage, and merchant list query behavior.
- No breaking changes.

## Capabilities

### New Capabilities

- `home-search-filter`: Allows users to search and filter merchants from the home page with clear result feedback and low-friction reset/refine behavior.

### Modified Capabilities

- None.

## Impact

- Frontend:
  - `frontend/src/views/HomeView.vue`
  - possibly `frontend/src/api/merchant.js` or `frontend/src/api/clas.js` only if API helper parameters need normalization
- Backend:
  - `MerchantController.list`
  - `MerchantService.search`
  - only if current search/filter behavior needs small compatibility improvements
- Database:
  - No required schema change
  - Optional index recommendation for merchant category/status/search fields can be documented separately
- Conflict boundary:
  - Avoid modifying cart, order, payment, merchant console, admin pages, global router, and global style unless absolutely necessary
