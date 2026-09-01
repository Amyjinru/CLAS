## Why

UC05–UC08 already have executable implementation code, but the repository does not yet provide one auditable chain from use-case intent through detailed design, code symbols, planned tests, and verification evidence. This change establishes that documentation baseline without changing runtime behavior or executing tests in this phase.

## What Changes

- Add complete use-case specifications for UC05–UC08, including actors, triggers, preconditions, main success flows, alternate/exception flows, and observable results.
- Add design views for each use case, including interaction/state diagrams, API boundaries, persistence objects, and implementation symbol mappings.
- Add uniquely numbered requirement, flow, design, code, and test identifiers and a bidirectional traceability matrix.
- Record existing automated test evidence separately from planned unit, API, and E2E cases so an unexecuted test is never presented as passing evidence.
- Add run-readiness prerequisites and future verification commands, while explicitly leaving test execution out of scope for this documentation-only phase.
- No production code, API, database schema, or test code is changed.

## Capabilities

### New Capabilities

- `uc05-favorites-notifications`: Document the user flow for favoriting merchants and receiving, reading, deleting, and following business notifications.
- `uc06-merchant-onboarding-audit`: Document merchant application, administrator review, status transitions, and audit-history traceability.
- `uc07-merchant-operations`: Document merchant profile, business-state, product-category, and product lifecycle management.
- `uc08-coupon-purchase`: Document ordinary coupon-assisted checkout and group-deal voucher purchase, payment, redemption, expiry, and refund behavior.
- `uc-traceability-baseline`: Define the documentation, run-readiness, evidence-status, and end-to-end traceability rules shared by UC05–UC08.

### Modified Capabilities

None. Existing runtime requirements are not changed; this change documents the current implementation and identifies future test gaps.

## Impact

- Documentation: new OpenSpec proposal, requirements, design diagrams, test design, and traceability records under this change.
- Referenced backend areas: favorite, notification, merchant, product, coupon, order, payment, and deal controllers/services/entities/mappers.
- Referenced frontend areas: merchant detail and profile/notification views; merchant registration, audit, profile, product, console, deal, cart, and order views; corresponding API clients and routes.
- Referenced tests: Spring integration tests, frontend notification unit tests, API smoke specifications, and Playwright E2E suites.
- Runtime impact: none.
