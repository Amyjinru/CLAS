# Pending Payment Timeout Specification

## ADDED Requirements

### Requirement: Pending payment orders expire automatically

The backend SHALL automatically cancel `PENDING_PAYMENT` orders older than the configured timeout.

#### Scenario: Old unpaid order expires

- **GIVEN** an order is in `PENDING_PAYMENT`
- **AND** its `createTime` is older than the configured timeout
- **WHEN** the timeout scanner runs
- **THEN** the order status becomes `CANCELED`

### Requirement: Expired unpaid orders release reserved resources

When an unpaid order is expired, the backend SHALL release any reserved coupon and notify the user.

#### Scenario: User is notified

- **WHEN** a pending-payment order expires
- **THEN** the user receives an `ORDER_STATUS` notification for that order
