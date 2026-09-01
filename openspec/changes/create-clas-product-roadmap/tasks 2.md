## 1. P0 Core Stabilization

- [ ] 1.1 Standardize final demo account, password, role, and auth header conventions across README, docs, seed data, and tests.
- [ ] 1.2 Improve login and role permission behavior with clearer unauthorized and forbidden responses.
- [ ] 1.3 Verify and document the full user order flow from merchant browsing to cart, order creation, payment, fulfillment, completion, and review.
- [ ] 1.4 Improve cart and order validation for stock, single-merchant grouping, delivery fee, min order price, and invalid products.
- [ ] 1.5 Improve payment flow for duplicate payment, payment failure simulation, and payment timeout handling.
- [ ] 1.6 Improve merchant fulfillment flow with visible status transitions, rejection reason, and delivery progress.
- [ ] 1.7 Improve merchant onboarding and admin audit flow with audit detail, progress status, and audit notification.
- [ ] 1.8 Improve address, notification, announcement, and order-detail user experience for empty, loading, and error states.

## 2. P0 Data and Test Readiness

- [ ] 2.1 Ensure `database/schema.sql`, `database/migration-20260608.sql`, and H2 test schema stay synchronized.
- [ ] 2.2 Add or document key status machines for merchant, product, order, payment, refund, deal, booking, and review report.
- [ ] 2.3 Review transaction boundaries for order creation, stock changes, payment, refund, and deal purchase/redeem.
- [ ] 2.4 Add focused backend tests for the main order, refund, booking, deal, and permission flows.
- [ ] 2.5 Run backend tests, frontend build, and one manual P0 demo verification after stabilization.

## 3. P1 Commercial Features

- [ ] 3.1 Implement or design a minimal coupon/discount module with receive, validity, threshold, and order deduction behavior.
- [ ] 3.2 Enhance group deals with refund, expiry, usage rules, and redeem history.
- [ ] 3.3 Add merchant product categories and optional product specification support.
- [ ] 3.4 Enhance search and discovery with combined merchant/product search, distance sorting, price filters, and search history.
- [ ] 3.5 Improve merchant business-hours management and prevent ordering outside valid service time.
- [ ] 3.6 Improve delivery rules with distance-based fee, delivery range validation, and estimated arrival time.
- [ ] 3.7 Improve refund after-sales with categorized reasons, refund progress, and optional admin intervention.
- [ ] 3.8 Add merchant business analytics for sales, order trends, hot products, ratings, and refund rate.
- [ ] 3.9 Add platform operation configuration for categories, platform notices, fee rules, and basic governance settings.

## 4. P1 Operations and Governance

- [ ] 4.1 Add customer service or work-order workflow for consultation, complaint, reply, and close status.
- [ ] 4.2 Add content governance workflow for product takedown, merchant violation handling, and audit traces.
- [ ] 4.3 Add data export for admin orders, users, merchants, reviews, or statistics.
- [ ] 4.4 Improve mobile responsiveness for core user pages and desktop density for admin pages.
- [ ] 4.5 Update user manual and test report for each P1 function implemented.

## 5. P2 Highlight Selection

- [ ] 5.1 Select at most two P2 highlights after P0 stabilization and P1 scope decision.
- [ ] 5.2 If selected, deepen map-based delivery with location, route, distance, estimated time, and fallback behavior.
- [ ] 5.3 If selected, build an operation dashboard mode with visual sales, order, merchant, and map indicators.
- [ ] 5.4 If selected, add JWT/Spring Security and BCrypt password storage with regression tests.
- [ ] 5.5 If selected, add API automation or E2E smoke tests for the main demo path.
- [ ] 5.6 If selected, add simple recommendation based on category, favorites, and order history.
- [ ] 5.7 If selected, add rule-based or knowledge-base customer FAQ assistant.

## 6. Roadmap Documentation and Acceptance

- [ ] 6.1 Convert the P0/P1/P2 roadmap into a team-facing project plan with owners and target dates.
- [ ] 6.2 Mark every roadmap item as implemented, partially implemented, or not implemented in the final planning document.
- [ ] 6.3 For every partially implemented item, list concrete remaining backend, frontend, database, test, and documentation work.
- [ ] 6.4 For every missing item selected for implementation, create a separate OpenSpec change before coding.
- [ ] 6.5 Re-run final verification and update final course documents after selected roadmap items are completed.
