## Why

CLAS currently lets merchants mark orders as delivered directly and has no rider identity, dispatch, real-time tracking, settlement, or rider-governance capability. The summer-course deliverables require a complete, testable business flow with traceable design and evidence, so the delivery role must become a secure multi-party workflow rather than a UI-only addition.

## What Changes

- Add an approved rider identity that one phone-number account can hold alongside user, merchant, or administrator identities; the active identity is selected after login.
- Add the rider application and administrator review workflow, including encrypted identity-number storage, access audit records, rider enablement, and configurable concurrent-delivery limits.
- Replace merchant-only delivery completion with a rider-owned workflow: nearby task pool, atomic claim, up to three active deliveries, manual route ordering, pickup, delivery, abandon-before-pickup, and controlled reassignment.
- Add live rider location, AMap route/ETA integration with a graceful fallback, delivery promise windows, and an overdue rule that affects only rider score and delivery commission.
- Add order-scoped user-to-rider chat, simulated privacy-call sessions, optional one-time tips, rider reviews, settlement/withdrawal records, and daily performance metrics.
- **BREAKING** Change delivery authorization and delivery-status semantics: merchant acceptance creates a rider-claimable order; merchants no longer complete the physical delivery.
- **BREAKING** Extend protected authorization to use a JWT active role so one account can safely switch among its approved identities.

## Capabilities

### New Capabilities

- `rider-identity-governance`: Rider application, sensitive identity data protection, review, role activation, and administrator governance.
- `rider-delivery-operations`: Dispatch, multi-order delivery state machine, tracking, scheduling, reassignment, promise windows, and overdue handling.
- `delivery-collaboration-settlement`: User-rider contact, privacy-call simulation, optional tips, rider reviews, settlement, withdrawals, and daily rider metrics.

### Modified Capabilities

- `strict-bearer-auth`: JWT authorization must carry and enforce the currently selected approved identity for a multi-role account.
- `transaction-consistency`: Order delivery assignment and rider financial mutations require atomic conditional transitions and idempotency guarantees.
- `data-integrity-governance`: New rider, delivery, financial, and communication relationships need protected constraints and documented lifecycle invariants.

## Impact

- Affects the user/role model, JWT issuance and route authorization, order schema/state machine, merchant acceptance behavior, user order tracking, notifications, chat persistence, and administrator operations.
- Adds rider controllers/services/mappers/entities, migrations and test schema updates, rider-facing routes/components, and administrator/user integration endpoints.
- Reuses the existing AMap JavaScript loader and `AmapRouteService`; deployment must provide existing AMap environment variables plus `RIDER_IDENTITY_ENCRYPTION_KEY`.
- Requires coordinated changes across the identity, merchant, user-order, chat, admin, database, test, CI, and documentation owners.
