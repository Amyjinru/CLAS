## 1. Backend Detail API

- [x] 1.1 Add a `DealService` method to fetch one group-buy deal by id and return a clear business error when it does not exist.
- [x] 1.2 Add `GET /api/deals/{dealId}` in `DealController`, ensuring it does not conflict with existing order and merchant routes.
- [x] 1.3 Add or update backend tests for successful single-deal loading and missing-deal error handling.

## 2. Frontend API and Routing

- [x] 2.1 Add `getDeal(id)` to `frontend/src/api/deal.js` and export it through the shared API barrel.
- [x] 2.2 Create a lazy-loaded `DealDetailView.vue` route at `/deals/:id` with USER role protection and an appropriate page title.
- [x] 2.3 Ensure direct URL loading shows loading, loaded, and unavailable/error states without requiring prior navigation from `/deals`.

## 3. Detail Page Experience

- [x] 3.1 Build the detail layout with title, merchant identity, description, price comparison, stock, validity, redemption guidance, and purchase notes.
- [x] 3.2 Reuse existing merchant lookup patterns to display merchant names and fall back gracefully when merchant data is unavailable.
- [x] 3.3 Implement the purchase action using `buyDeal`, disable it while loading or sold out, and redirect successful purchases to `/payment/deal/{orderId}`.
- [x] 3.4 Handle failed purchase attempts by staying on the detail page and relying on existing error messaging.

## 4. List Page Integration and Verification

- [x] 4.1 Update `/deals` cards to expose a clear "查看详情" action that navigates to `/deals/{dealId}`.
- [x] 4.2 Remove or de-emphasize direct purchase from list cards so the detail page becomes the primary purchase review step.
- [x] 4.3 Run the frontend build and relevant backend tests.
- [x] 4.4 Manually verify list-to-detail navigation, direct detail URL loading, sold-out disabled state, and successful purchase handoff to payment.
