## ADDED Requirements

### Requirement: Atomic Multi-Order Creation
The system MUST commit all merchant orders in a selected checkout together or roll back all order, order-item, cart, and coupon-reservation mutations.

#### Scenario: Later merchant creation fails
- **WHEN** an earlier merchant order is created but a later merchant group fails validation or persistence
- **THEN** the earlier order and every related mutation are rolled back

### Requirement: Idempotent Aggregate Payment Retry
The system MUST preserve order-level payment idempotency when multiple orders are submitted through one aggregate payment request.

#### Scenario: Aggregate payment retried
- **WHEN** an aggregate payment request is retried with paid and pending orders
- **THEN** paid orders return their successful result and only pending orders perform payment mutations
