## Why

The user group-buying flow currently jumps from the deal list directly to purchase, so users cannot inspect a deal's merchant context, usage rules, validity, refund notes, or stock state before paying. Adding a dedicated detail page reduces purchase uncertainty and gives the group-buying module the same review-and-confirm flow users expect from product and merchant pages.

## What Changes

- Add a user-facing group-buy detail route reachable from the `/deals` list through a clear "查看详情" action.
- Present deal summary, merchant identity, price comparison, stock, validity, redemption guidance, purchase notes, and unavailable states in one focused detail screen.
- Add or expose a single-deal read API so the detail page can load directly by deal id, including safe handling for missing, off-sale, sold-out, or closed-merchant cases.
- Move the primary purchase action onto the detail page while keeping list browsing lightweight and scan-friendly.
- Preserve the existing payment flow after purchase by redirecting successful purchases to `/payment/deal/:orderId`.

## Capabilities

### New Capabilities
- `group-buy-detail-experience`: User-facing group-buy detail browsing, state display, and purchase handoff.

### Modified Capabilities

## Impact

- Frontend user routes: add `/deals/:id` and update `/deals` cards to link to detail.
- Frontend API layer: add a `getDeal(id)` client method and reuse existing purchase/payment calls.
- Frontend views: add a detail view with loading, empty/error, sold-out, off-sale, and purchase-in-progress states.
- Backend API: add `GET /api/deals/{id}` or equivalent single-deal endpoint if no suitable endpoint exists.
- Backend service tests and frontend smoke/build checks should cover direct detail loading, unavailable deals, and successful purchase handoff.
