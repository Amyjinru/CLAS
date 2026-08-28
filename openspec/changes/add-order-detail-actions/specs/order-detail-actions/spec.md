# Order Detail Actions Specification

## ADDED Requirements

### Requirement: Order detail exposes contextual actions

The order detail page SHALL show user actions that match the current order status.

#### Scenario: Pending payment order

- **GIVEN** a pending-payment order
- **WHEN** the user opens its detail page
- **THEN** the page offers payment and cancellation actions

#### Scenario: Accepted order

- **GIVEN** an accepted order
- **WHEN** the user opens its detail page
- **THEN** the page offers completion and refund actions

#### Scenario: Completed order

- **GIVEN** a completed order
- **WHEN** the user opens its detail page
- **THEN** the page offers review and refund actions
