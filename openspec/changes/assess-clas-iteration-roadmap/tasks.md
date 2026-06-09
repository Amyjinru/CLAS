## 1. Scope Freeze and Source Audit

- [ ] 1.1 Confirm final project positioning as CLAS campus/community local life service platform.
- [ ] 1.2 Confirm final demo accounts, passwords, roles, and `Authorization` header convention.
- [ ] 1.3 Audit README, architecture notes, progress notes, test reports, schema, frontend routes, and backend controllers for inconsistent historical wording.
- [ ] 1.4 Create one canonical module inventory covering USER, MERCHANT, ADMIN, backend shared services, database, testing, and deployment.
- [ ] 1.5 Mark optional future features as out of final submission scope unless required by the course rubric.

## 2. Required Course Documents

- [ ] 2.1 Produce the final software detailed design specification with architecture, modules, APIs, database, state machines, permissions, and exception handling.
- [ ] 2.2 Produce the final consolidated test report from existing phase test reports and latest verification results.
- [ ] 2.3 Produce the formal deployment document from `note.md`, README, server configuration, database scripts, and startup commands.
- [ ] 2.4 Produce the user manual separated by ordinary user, merchant user, and administrator workflows.
- [ ] 2.5 Add a final document index that lists all submitted documents and their purpose.

## 3. Demo Stabilization

- [ ] 3.1 Define the main live demo path from login to merchant browsing, cart, order, payment, merchant processing, completion, and review.
- [ ] 3.2 Define secondary demo paths for merchant onboarding/audit, group deal purchase/redeem, service booking, refund, and review governance.
- [ ] 3.3 Reset seed data in `database/schema.sql` so the demo paths work from a clean database.
- [ ] 3.4 Verify frontend role redirects for USER, MERCHANT, and ADMIN routes.
- [ ] 3.5 Add or document visible success, failure, empty, and loading states for the most important demo pages.

## 4. Database and State Model Review

- [ ] 4.1 Document the 16 business tables with field meanings, primary keys, logical relationships, and business constraints.
- [ ] 4.2 Document status machines for merchant audit, product lifecycle, order/payment/delivery, refund, deal order, booking, and review report.
- [ ] 4.3 Check schema, migration script, and H2 test schema for field drift.
- [ ] 4.4 Propose low-risk indexes for common queries such as orders by user, orders by merchant/status, products by merchant/status, and notifications by user/read flag.
- [ ] 4.5 Record known database trade-offs such as `user.phone` primary key, logical foreign keys, money unit in cents, and one merchant per account.

## 5. Frontend and Backend Architecture Review

- [ ] 5.1 Document backend layering from Controller to Service to Mapper to MySQL.
- [ ] 5.2 Document current authentication and RBAC design, including why it is demo-level and what a JWT/Spring Security upgrade would change.
- [ ] 5.3 Review key services for ownership checks, validation, transaction boundaries, and state transition checks.
- [ ] 5.4 Document frontend route structure, role navigation, API module split, session storage, and reusable components.
- [ ] 5.5 Identify small architecture fixes that improve consistency without destabilizing the demo.

## 6. Verification and Final Packaging

- [ ] 6.1 Run backend tests with `mvn test`.
- [ ] 6.2 Run frontend build with `npm run build`.
- [ ] 6.3 Run a manual full-process demo against the final seed data.
- [ ] 6.4 Update the final test report with latest pass/fail totals and known limitations.
- [ ] 6.5 Package source code, database scripts, documents, screenshots, and optional demo video/PPT into the final submission structure.

## 7. Optional Excellence Enhancements

- [ ] 7.1 Choose at most two optional highlights after required documents and demo are stable.
- [ ] 7.2 If selected, improve map-based delivery display with distance, route, or estimated time evidence.
- [ ] 7.3 If selected, improve admin dashboard charts and screenshots for presentation.
- [ ] 7.4 If selected, introduce JWT/Spring Security only after current role-protected flows are regression-tested.
- [ ] 7.5 If selected, add API collection or E2E smoke tests for the main demo path.
