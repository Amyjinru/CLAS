## 1. Team Setup

- [ ] 1.1 Confirm five member ownership lanes: user frontend, merchant frontend, admin frontend, transaction backend, database/test/docs.
- [ ] 1.2 Create feature branches for the five lanes using clear owner-based names.
- [ ] 1.3 Agree on shared-file lock rules for router, API client, auth, result handling, and schema files.
- [ ] 1.4 Agree on API contract workflow before frontend/backend parallel work starts.

## 2. Person-Based P0 Sprint

- [ ] 2.1 Assign A to the user discovery and profile package: home search/filter polish, merchant detail polish, profile address/favorite/notification improvements.
- [ ] 2.2 Assign B to the merchant operations package: product management polish, merchant fulfillment workspace, deal and booking management polish.
- [ ] 2.3 Assign C to the admin governance package: merchant audit details, dashboard polish, admin users/orders/reviews/announcements improvements.
- [ ] 2.4 Assign D to the transaction flow package: cart validation, order creation rules, payment idempotency, refund status flow.
- [ ] 2.5 Assign E to the engineering support package: schema/migration/H2 synchronization, seed data, tests, task documentation, and merge support.

## 3. Person-Based P1 Sprint

- [ ] 3.1 Let A extend the same user package with combined search, distance/price sorting, mobile adaptation, and user-side empty states.
- [ ] 3.2 Let B extend the same merchant package with product categories, group-deal redeem history, booking filters, and business-hours management.
- [ ] 3.3 Let C extend the same admin package with analytics, operation configuration, data export, and governance screens.
- [ ] 3.4 Let D extend the same transaction package with coupon MVP, delivery fee/range rules, and after-sales refund enhancement.
- [ ] 3.5 Let E extend the support package with indexes, migration batching, API/test documentation, and final verification scripts.

## 4. P2 Highlight Selection

- [ ] 4.1 Select one P2 highlight pair only after P0 is stable.
- [ ] 4.2 If selecting map and dashboard, assign map delivery to A/D and operation dashboard to C.
- [ ] 4.3 If selecting security and automation, assign JWT/BCrypt to D/E and API tests to E/D.
- [ ] 4.4 If selecting recommendation and FAQ, assign recommendation to D/A and FAQ assistant to A/C.
- [ ] 4.5 Keep P2 work on separate branches until the main demo path passes regression.

## 5. Database and API Coordination

- [ ] 5.1 Let E maintain one schema change queue for `schema.sql`, migration SQL, and H2 test schema.
- [ ] 5.2 Require A/B/C/D to submit database field requirements to E before modifying entities or mappers.
- [ ] 5.3 Let D publish transaction-related request/response DTO contracts before A or B finalizes UI integration.
- [ ] 5.4 Keep additive database changes preferred over destructive schema changes.

## 6. Merge and Verification

- [ ] 6.1 Merge foundational backend and database changes before dependent frontend branches.
- [ ] 6.2 Keep each PR scoped to one task-pool item or one ownership lane.
- [ ] 6.3 Run `mvn test` after backend/database merges.
- [ ] 6.4 Run frontend build after frontend merges.
- [ ] 6.5 Run manual smoke tests for login, user order, merchant fulfillment, admin audit, refund, deal, and booking flows before sprint close.
