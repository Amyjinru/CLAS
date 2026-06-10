## 1. Frontend Search Controls

- [x] 1.1 Add `loading`, `onlyDeliverable`, and result metadata state to `HomeView.vue`.
- [x] 1.2 Wire `onlyDeliverable` into `listMerchants` params only when a usable location or address exists.
- [x] 1.3 Add a visible deliverable-only switch or checkbox with disabled/warning behavior when no location is available.
- [x] 1.4 Add a reset filters action that clears keyword, category, deliverable-only, and restores the default sort.
- [x] 1.5 Keep existing keyword, category, address, location, and sort controls working after the new controls are added.

## 2. Frontend Result Feedback

- [x] 2.1 Show loading state while merchants are being fetched.
- [x] 2.2 Show result count after loading completes.
- [x] 2.3 Show active filter tags or a readable active-filter summary.
- [x] 2.4 Show an empty state when no merchants match, including reset and change-location actions.
- [x] 2.5 Improve merchant card delivery availability display without changing the card's route target.

## 3. Backend Compatibility

- [x] 3.1 Verify `/api/merchant/list` accepts `keyword`, `category`, `sort`, `lat`, `lng`, `addressId`, and `onlyDeliverable`.
- [x] 3.2 If needed, trim keyword/category inputs in `MerchantService.search`.
- [x] 3.3 If needed, normalize unknown sort values to score sorting.
- [x] 3.4 Avoid database schema changes for this feature.

## 4. Verification

- [x] 4.1 Test keyword search by merchant name, address, and category.
- [x] 4.2 Test category filter with and without keyword.
- [x] 4.3 Test all sort modes: distance, score, price, latest.
- [x] 4.4 Test deliverable-only with selected location/address.
- [x] 4.5 Test deliverable-only behavior without location.
- [x] 4.6 Test reset filters returns to default browsing state.
- [x] 4.7 Test empty result state with an impossible keyword.
- [x] 4.8 Run frontend build or dev-server smoke test after implementation.

## 5. Conflict Avoidance

- [x] 5.1 Do not edit cart, payment, merchant-console, admin, or database schema files for this change.
- [x] 5.2 Coordinate before modifying `frontend/src/router/index.js`, global CSS, or shared API client files.
- [x] 5.3 Keep the final commit focused on home search/filter enhancement only.
