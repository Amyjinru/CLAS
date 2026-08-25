## 1. Shared identity and migration foundation

- [x] 1.1 Add an additive `user_role` migration, backfill current USER/MERCHANT/ADMIN accounts, and retain legacy role compatibility.
- [x] 1.2 Add rider application, profile, audit-log, location-history, settlement, withdrawal, tip, review, daily-metric, exception, call-session, and conversation migrations with required unique keys and indexes.
- [x] 1.3 Extend `orders` and merchant data with rider assignment, delivery timestamps, sequence, preparation snapshot, promise window, ETA, commission, reassignment count, and `default_prepare_minutes` fields.
- [x] 1.4 Add equivalent tables, columns, indexes, and synthetic multi-role seed data to `backend/src/test/resources/schema-test.sql`.
- [x] 1.5 Add a documented migration integrity check for duplicate tips/reviews, duplicate rider profiles, settlement source uniqueness, and legacy-role backfill.

## 2. Authentication, rider application, and administration

- [x] 2.1 Extend login/session responses to return approved and pending account identities without exposing sensitive rider application data.
- [x] 2.2 Implement identity-switch token issuance with an active-role JWT claim and update interceptor/context/`@RequireRole` behavior.
- [x] 2.3 Add rider-application DTO validation, AES-GCM identity encryption, masking, simulated credential-link handling, and resubmission history.
- [x] 2.4 Implement rider application/profile APIs and enforce approved rider status for every rider business endpoint.
- [x] 2.5 Implement administrator APIs for rider applications, approval/rejection, enable/disable, capacity range 1–10, audited identity-number reveal, and profile inspection.
- [x] 2.6 Add administrator rider-management UI integration points: application queue, capacity control, exception view, withdrawal queue, and dashboard summaries.
- [x] 2.7 Add `RIDER_IDENTITY_ENCRYPTION_KEY` to local/server environment examples and fail safely outside test profiles when it is absent.

## 3. Merchant acceptance and rider dispatch

- [x] 3.1 Add merchant registration/profile support to create and update `default_prepare_minutes` in the agreed range.
- [x] 3.2 Change merchant acceptance to snapshot preparation time, create the initial twenty-minute promise window, and expose the order as `AVAILABLE`.
- [x] 3.3 Retire or migrate merchant-owned `/deliver` behavior so only the assigned rider can record pickup and delivery.
- [x] 3.4 Implement rider online/offline status and authenticated location upsert/history APIs.
- [x] 3.5 Implement nearby task-pool and recommendation queries with five-kilometre rider-to-merchant filtering and safe response masking.
- [x] 3.6 Implement atomic task claim with approved/online/capacity/status checks and a deterministic concurrent-claim failure response.
- [x] 3.7 Implement rider active-delivery listing, system recommendation, persisted manual sequence permutation, and ETA/late-risk recalculation.
- [x] 3.8 Implement pickup, delivery, pre-pickup abandonment, user cancellation release, and post-pickup exception guards through one delivery state service.

## 4. Route, promise, tracking, and notifications

- [x] 4.1 Reuse `AmapRouteService` to calculate merchant-to-user and rider-to-merchant-to-user estimates with straight-line fallback metadata.
- [x] 4.2 Implement initial promise-window generation, retained promise-end semantics, and current ETA calculation.
- [x] 4.3 Implement order tracking API authorization for order owner, assigned rider, merchant owner, and administrator; deny all unrelated accounts.
- [x] 4.4 Add rider workbench maps using existing AMap loader/geolocation/driving plugins and submit location while online with active work.
- [x] 4.5 Add user-order tracking integration that polls active rider tracking every five seconds and stops live location after delivery.
- [x] 4.6 Emit privacy-safe notifications for application results, acceptance, assignment, pickup, delivery, cancellation, abandonment, overdue, tip, review, and withdrawal decisions.

## 5. Collaboration and privacy call simulation

- [x] 5.1 Add `chat_conversation` migration and adapt existing user-merchant messages without losing their historical access behavior.
- [x] 5.2 Implement USER_RIDER order conversation authorization, message APIs, and polling integration for the user and rider clients.
- [x] 5.3 Close new USER_RIDER messages after delivery while keeping conversation history readable to authorized participants.
- [x] 5.4 Implement the ten-minute simulated call-session API, masked-number response, expiry, and audit trail; do not return real user phone numbers.

## 6. Settlement, tips, reviews, overdue, and performance

- [x] 6.1 Define and implement rider commission creation on delivery, pending-to-withdrawable settlement on user confirmation, and immutable balance updates.
- [x] 6.2 Implement one-time optional 1–5000-cent tips with an idempotency key, pending settlement, and no automatic refund behavior.
- [x] 6.3 Implement rider withdrawal requests against withdrawable balance, frozen-balance transitions, and administrator approval/rejection workflows.
- [x] 6.4 Implement one-review-per-confirmed-order rider reviews with 1–5 star validation, labels, and rider-visible results.
- [x] 6.5 Implement the overdue scanner with unique exception creation, five-point performance deduction, and 20-percent commission deduction exactly once.
- [x] 6.6 Implement daily Asia/Shanghai metrics update/archive at 00:10, the agreed weighted score formula, provisional no-review baseline, grade calculation, and auditable manual adjustments.
- [x] 6.7 Add rider income, withdrawal, review, and performance views plus administrator summary and exception/withdrawal views.

## 7. Cross-flow protection and regression integration

- [x] 7.1 Update user cancellation and refund rules so pre-pickup cancellation releases but never reassigns, while post-pickup cancellation is rejected into existing after-sales handling.
- [x] 7.2 Update order timeline/status labels and merchant/user/rider navigation to represent every new delivery state consistently.
- [x] 7.3 Ensure notifications, chat, review, refund, and order-completion flows use active-role authorization and do not leak phone, identity, bank, or location data.
- [x] 7.4 Add API client functions and route guards for rider routes while leaving the separately owned identity-selection page to its owner.

## 8. Verification, delivery evidence, and documentation

- [ ] 8.1 Add unit tests for identity approval, masking/encryption, state transitions, capacity, sequence validation, promise/ETA fallback, metrics, and settlement calculations.
- [ ] 8.2 Add integration tests for concurrent claim, unrelated tracking access, pre/post-pickup cancellation, one-time tips/reviews, overdue idempotency, and withdrawal approval/rejection.
- [ ] 8.3 Add end-to-end tests with one user, merchant, administrator, and two riders covering review, claim race, three-task limit, map fallback, delivery, tip, review, overdue deduction, and withdrawal.
- [ ] 8.4 Run backend tests, frontend build, container startup, and CI; preserve logs and links as acceptance evidence.
- [ ] 8.5 Update UC16 requirements, overview design, detailed design, traceability table, test report, README role accounts, deployment variables, and final-demo script.
- [ ] 8.6 Capture dated board/statistics screenshots, representative task/PR/test screenshots, and the final CI success evidence required by the course.
