# Order Status Timeline Specification

## ADDED Requirements

### Requirement: Order lifecycle timestamps are recorded

The backend SHALL record timestamps for key order lifecycle transitions.

#### Scenario: Paid order records paid time

- **GIVEN** a pending-payment order
- **WHEN** payment succeeds
- **THEN** the order has `paidAt`

#### Scenario: Merchant handling records timestamps

- **GIVEN** a paid order
- **WHEN** the merchant accepts and marks delivery
- **THEN** the order has `acceptedAt` and `deliveredAt`

#### Scenario: User completion records completed time

- **GIVEN** an accepted order
- **WHEN** the user completes it
- **THEN** the order has `completedAt`

### Requirement: Order detail renders a timeline

The user order detail page SHALL render timestamped lifecycle nodes from the order detail payload.

#### Scenario: User views a progressed order

- **GIVEN** an order has lifecycle timestamps
- **WHEN** the user opens the order detail page
- **THEN** the page displays a chronological timeline of those lifecycle nodes
