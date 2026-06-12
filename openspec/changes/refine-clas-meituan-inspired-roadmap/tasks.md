## 1. Source Review and Current-State Audit

- [x] 1.1 Review the Meituan blueprint sections on UX, architecture, database/indexing, frontend routes, API design, roadmap phases, order state machine, and dispatch scoring.
- [x] 1.2 Review current CLAS `dev` capabilities across README, routes, controllers, services, database schema, and active OpenSpec changes.
- [x] 1.3 Create a capability evidence matrix that marks each Meituan-inspired idea as implemented, partially implemented, missing, or intentionally out of scope for CLAS.

## 2. Roadmap Track Refinement

- [x] 2.1 Refine the experience foundation track with home discovery, merchant detail, product spec modal, skeleton states, and cart behavior.
- [x] 2.2 Refine the transaction reliability track with order/payment/refund/deal state machines, stock validation, coupon consistency, timeout handling, and idempotency.
- [x] 2.3 Refine the fulfillment and delivery track with merchant delivery states, ETA/range/fee logic, route fallback, and optional simulated rider dispatch.
- [x] 2.4 Refine the growth and marketing track with coupons, group deals, membership, points, search, hot keywords, and category filters.
- [x] 2.5 Refine the operations and governance track with admin configuration, audit logs, merchant analytics, dashboard drill-down, and export.
- [x] 2.6 Refine the engineering hardening track with request IDs, domain error codes, pagination conventions, cache/index review, smoke tests, and optional real-time updates.

## 3. Scope and Priority Decisions

- [x] 3.1 Mark which items are required for the next course-demo milestone and which are optional highlights.
- [x] 3.2 Defer full microservices, Kubernetes, RocketMQ, Elasticsearch, MongoDB, real payment, and real rider dispatch unless a later change explicitly selects them.
- [x] 3.3 Choose the top three implementation candidates that give the best value-to-risk ratio for the next iteration.
- [x] 3.4 Define acceptance criteria for each chosen candidate before any coding begins.

## 4. Follow-Up OpenSpec Planning

- [x] 4.1 Split selected roadmap candidates into separate narrow OpenSpec change names.
- [x] 4.2 For each candidate, identify impacted frontend routes, backend modules, database tables, tests, and documentation.
- [x] 4.3 Ensure every future implementation change includes verification tasks for backend tests, frontend build or UI check, and README/test report updates.
- [x] 4.4 Keep this roadmap change planning-only and do not implement runtime behavior from it directly.

## 5. Final Planning Review

- [x] 5.1 Review the refined roadmap against the Meituan blueprint to ensure selected adaptations are represented.
- [x] 5.2 Review the refined roadmap against CLAS current state to remove duplicates and stale assumptions.
- [x] 5.3 Confirm the final roadmap has clear track order, priority gates, non-goals, risks, and next-step OpenSpec candidates.
