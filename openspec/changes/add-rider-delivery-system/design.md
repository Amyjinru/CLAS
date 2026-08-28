## Context

The current monolith has a single `user.role`, merchant-owned delivery completion, order-level user-merchant polling chat, route estimation through AMap, and no rider tables. The change crosses authentication, orders, merchant operations, user tracking, chat, notifications, finance, administration, test schema, and CI. A user can hold several identities but the client must operate under exactly one active identity at a time.

## Goals / Non-Goals

**Goals:**

- Deliver a secure, testable rider lifecycle from application review through multi-order delivery, settlement, and daily performance.
- Preserve the existing order payment, refund, completion, and user-merchant messaging behavior where it does not conflict with rider fulfillment.
- Make every delivery-state and money mutation authorized, atomic, idempotent, auditable, and observable in tests.
- Reuse the existing AMap JavaScript loader and server-side `AmapRouteService`.

**Non-Goals:**

- No real telecom virtual-number connection; this release supplies only a privacy-preserving simulated call session.
- No automatic user compensation, refund, cancellation, or reassignment triggered by overdue delivery.
- No delivery after a rider has picked up an order may be auto-reassigned.
- No direct connection to a real payment or bank withdrawal provider; the ledger and approval workflow are simulated.
- No unrestricted dispatch: active deliveries are limited by an administrator-configurable maximum, defaulting to three.

## Decisions

### Multi-role account and active-role JWT

Create `user_role` with a unique `(user_id, role)` and an approval status. A login first returns the account's available identities; identity selection obtains a JWT whose active role is a claim. `AuthInterceptor` validates the token, verifies that the selected identity remains approved/enabled, and `@RequireRole` authorizes the active role.

This avoids duplicate phone-number accounts and prevents a client-provided role from escalating access. Replacing the existing role column immediately is avoided: it remains a migration compatibility field until every existing consumer uses `user_role`.

### Rider application and sensitive identity data

Applications are append-only snapshots in `rider_application`; the approved current data belongs in `rider_profile`. Store identity number as AES-GCM ciphertext plus a masked representation. The encryption key is `RIDER_IDENTITY_ENCRYPTION_KEY`; plaintext MUST NOT be logged or returned to ordinary APIs. An administrator's complete-value access requires a purpose and creates an audit event.

This is selected over plain storage because the user explicitly requires the full identity number, while the project must not expose it in test output, logs, or ordinary pages.

### Delivery state ownership

Merchant acceptance changes an order to `ACCEPTED` and delivery status `AVAILABLE`; it snapshots the merchant profile's configurable `default_prepare_minutes`. A rider owns physical fulfillment with `ASSIGNED_WAITING_MEAL`, `DELIVERING`, and `DELIVERED`. The existing merchant `/deliver` semantics are removed or migrated to the availability transition.

The rider can claim before food is ready and waits at the merchant if necessary. A rider can abandon only before pickup, returning the task to `AVAILABLE`; a post-pickup abandon becomes an administrator-managed exception. User cancellation before pickup cancels the order and releases the rider, never reassigns it. Post-pickup cancellation is rejected and follows the refund process.

### Atomic dispatch and multi-order scheduling

Claim uses a short transaction/conditional update: rider is approved and online, active order count is below the rider limit, and `orders.rider_id IS NULL` with delivery status `AVAILABLE`. The first concurrent claimant wins. Riders can hold up to their configured capacity (default three).

Rider online status is independent from accepting-orders status. Location reporting requires online status. A rider starts accepting orders explicitly; only then may they claim a task. Ending acceptance always succeeds and stops only future claims, while already assigned deliveries remain actionable until completed or validly abandoned before pickup.

The system recommends an order sequence prioritizing picked-up orders, then ready/at-risk pickups, then travel distance. Riders can submit a permutation of their own active tasks; the system records the old and new order and recalculates ETAs. A five-kilometre rider-to-merchant filter limits the task pool.

### ETA, promise window, and overdue handling

At merchant acceptance, generate a twenty-minute promise window centered on the estimated delivery time: snapshot preparation time plus dispatch buffer plus AMap merchant-to-user route duration. Once assigned, update the predicted arrival from rider-to-merchant-to-user routing while retaining the original promise end for late determination. If AMap fails, use straight-line distance and mark route capability unavailable; state transitions remain functional.

An overdue scanner creates one unique `OVERDUE` exception per order. It deducts 5 performance points and 20% of the order's rider commission, but does not change order status, issue a compensation, send an auto-refund, or reassign the order.

### Tracking, communication, and privacy

The rider client reports location every 15 seconds while online with active work; the user tracks only their assigned rider between assignment and delivery, polling every five seconds. No endpoint returns the user's full phone number. User-rider chat is a distinct `USER_RIDER` order conversation from user-merchant chat. A simulated call-session endpoint returns only a masked display number and auditable session metadata.

Polling is retained because the existing chat implementation uses it and avoids adding a new message broker or WebSocket operational dependency during the first five-day scope.

### Ledger, tips, withdrawals, reviews, and metrics

Use append-only `rider_settlement` records rather than overwriting balances. On delivery, the rider commission is pending; on user confirmation, it becomes withdrawable. A user may voluntarily pay one 1–5000-cent tip after delivery; it becomes withdrawable on confirmation. Withdrawals transfer withdrawable to frozen balance and require administrator approval. Each order permits one rider review after confirmation.

Daily metrics are recomputed/updated in Asia/Shanghai and archived at 00:10. The final score is the weighted base score (punctuality 40%, rating 25%, order count 15%, net income 10%, completion 10%) minus 5 for each overdue order, floored at zero. The absence of reviews uses a visibly labelled provisional 4.0-star baseline.

## Risks / Trade-offs

- [Identity migration can break existing single-role consumers] → Keep the legacy role readable during staged migration; add multi-role compatibility tests before retiring it.
- [AMap configuration or service outage] → Existing environment variables are used; route failure degrades to straight-line estimates without blocking fulfillment.
- [Location permission denied or stale] → Mark tracking stale and retain state-flow capability; never fabricate a live location.
- [Sensitive identity data exposure] → Encrypt at rest, mask by default, audit privileged reads, use synthetic test values, and exclude keys from source control.
- [Concurrent claim or repeated timeout scan] → Conditional updates and unique source/exception constraints provide idempotency.
- [Chat schema change affects UC12 owner] → Introduce conversation records with a migration path; preserve existing user-merchant conversation behavior.
- [Simulated withdrawal/call could be mistaken for production integration] → Label both UI and API responses as simulated and retain audit evidence.

## Migration Plan

1. Add additive rider and multi-role tables, columns, indexes, and test-schema equivalents; seed synthetic user, merchant, administrator, and two rider accounts.
2. Add role-selection JWT support while retaining current role compatibility for existing sessions; test authorization boundaries.
3. Deploy merchant preparation-time profile support and rider order columns before switching merchant delivery behavior.
4. Enable rider dispatch/state APIs, then user tracking/chat/tip/review integration and administrator APIs.
5. Enable scheduled overdue and daily metrics only after idempotency constraints, seed data, and tests are present.
6. Roll back feature exposure by disabling rider routes/role activation; additive data remains intact. Do not roll back a migration by deleting settlement or audit history.

## Open Questions

- No blocking product questions remain. The real virtual-number provider remains intentionally out of scope; the call-session API is simulated until an approved provider and credentials are supplied.
