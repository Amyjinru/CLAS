## Why

CLAS already has a broad set of local life service features, but the team needs a product-oriented iteration roadmap to decide what to build next and what to polish first.

This change defines a complete P0/P1/P2 roadmap so future development can prioritize core completion, common commercial capabilities, and standout course-project highlights without losing scope control.

## What Changes

- Create a complete CLAS product roadmap grouped by:
  - P0 core functions
  - P1 common commercial functions
  - P2 project highlight functions
- Mark each roadmap item as implemented, partially implemented, or not implemented.
- For every missing or partially implemented item, describe the specific work still needed.
- Convert the roadmap into executable tasks for later `/opsx:apply` implementation.
- No runtime code, API, or database change is introduced by this proposal itself.

## Capabilities

### New Capabilities

- `clas-product-roadmap`: Defines the product iteration roadmap, completion status, and future implementation priorities for CLAS.

### Modified Capabilities

- None.

## Impact

- Affected planning artifacts:
  - Product roadmap proposal, design, specification, and implementation task list.
- Future implementation may affect:
  - user workflows: onboarding, discovery, order, payment, refund, review, booking, group deals
  - merchant workflows: onboarding, product operations, fulfillment, marketing, settlement, analytics
  - admin workflows: audit, governance, platform operations, analytics, configuration
  - database schema, API design, frontend route structure, UX states, test cases, and documentation
- No new external dependency is required for the planning phase.
