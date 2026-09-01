## Context

The current user group-buying flow is implemented around `frontend/src/views/DealsView.vue`, `frontend/src/api/deal.js`, and `backend/src/main/java/com/clas/controller/DealController.java`. Users can browse `/deals`, filter by merchant, and purchase immediately, but there is no direct route or single-deal API for reviewing one group-buy package before purchase.

The backend already stores the fields required for a practical detail screen in `GroupDeal`: merchant id, title, description, original price, deal price, stock, valid days, and status. Purchase validation also already checks sale status, stock, platform penalties, and merchant opening state, so the detail page should display availability without duplicating final purchase authority.

## Goals / Non-Goals

**Goals:**
- Add a direct user route `/deals/:id` that loads and renders one group-buy deal.
- Add a backend single-deal read endpoint so the route works from refresh, direct links, and notifications.
- Show enough detail for informed purchase: merchant, price, stock, validity, description, redemption guidance, and refund/usage notes.
- Keep `/deals` optimized for browsing by replacing direct purchase with a detail entry point.
- Reuse the existing buy and payment flow after the user confirms purchase.

**Non-Goals:**
- Redesign merchant-side group-buy creation or核销 workflows.
- Add image upload, SKU variants, multi-quantity purchases, comments, or recommendation logic.
- Change payment semantics or stock deduction timing.
- Change database schema unless an implementation audit finds a missing field that is already required by the spec.

## Decisions

1. Use `GET /api/deals/{dealId}` for the detail API.
   - Rationale: It matches the existing REST route shape under `/api/deals` and supports direct page load without fetching the full list.
   - Alternative considered: Query the list and filter client-side. This would make direct detail URLs dependent on list visibility and would hide the difference between missing and off-sale deals.

2. Return the existing `GroupDeal` shape for the first implementation.
   - Rationale: The detail page only needs fields already available on the entity, and the existing API returns this shape from the list endpoint.
   - Alternative considered: Create a richer `DealDetailResponse` with merchant data embedded. This is cleaner long term, but adds backend mapping work that is not required to solve the missing detail page.

3. Resolve merchant display data in the frontend using existing merchant APIs.
   - Rationale: `DealsView.vue` already loads merchants to map `merchantId` to names, and a detail page can reuse this pattern or use an existing single-merchant API if available.
   - Alternative considered: Backend joins merchant data into the detail response. That can be revisited if the page needs more merchant fields or if performance becomes an issue.

4. Keep final purchase validation server-side.
   - Rationale: The detail page can disable obvious unavailable actions for stock and status, but only `DealService.buy` and payment can enforce current stock, merchant hours, and user eligibility.
   - Alternative considered: Pre-check merchant open state on page load. This risks stale UI and duplicate rules unless a formal availability endpoint is introduced.

5. Update the list page to prioritize "查看详情".
   - Rationale: The problem is that users lack a specific detail screen. Keeping an immediate purchase button on cards would preserve the original uncertainty.
   - Alternative considered: Add both "查看详情" and "购买". This is acceptable later, but the initial change should guide users through the detail review path.

## Risks / Trade-offs

- Direct detail endpoint exposes off-sale or missing deals inconsistently with list filtering -> Return clear not-found/unavailable errors and render an actionable empty state.
- Merchant information may require an extra frontend request -> Reuse existing merchant data fetch and keep the page loading state clear; consider backend response composition later.
- Stock can change between detail view and payment -> Keep server-side purchase/payment validation authoritative and show interceptor/backend error messages.
- List page conversion may drop if direct purchase is removed -> Use a prominent detail CTA and a primary purchase button above the fold on the detail page.

## Migration Plan

1. Add backend service/controller read method for one deal.
2. Add frontend API method and route/component for `/deals/:id`.
3. Update `/deals` cards to link to detail.
4. Verify frontend build and backend tests. Add targeted tests for single-deal query where backend test patterns exist.
5. Rollback by removing the route/API method and restoring direct purchase on `/deals`; no data migration is required.

## Open Questions

- Should off-sale deals be visible to users who already have direct links, or should they render as not found?
- Should the detail page include merchant business hours and manual closed status in the first implementation if the existing merchant API exposes those fields?
