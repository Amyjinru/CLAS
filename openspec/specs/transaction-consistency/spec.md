## Purpose
Guarantee atomic payment, inventory, order status, and coupon state transitions through short transactions, conditional updates, and idempotent payment handling.

## Requirements

### Requirement: Short Transaction Payment Flow
The system SHALL process payment state changes without holding a database transaction during simulated or external payment waiting time.

#### Scenario: Mock payment delay outside transaction
- **WHEN** a mock payment includes simulated delay
- **THEN** the delay occurs before or between short database transactions and MUST NOT keep order, payment, product, or coupon rows locked during sleep

#### Scenario: Payment failure preserves pending order
- **WHEN** a mock payment fails
- **THEN** the payment record is marked `FAILED` and the order remains payable without stock deduction

### Requirement: Atomic Order Payment Transition
The system MUST move an order from `PENDING_PAYMENT` to paid only through conditional updates that verify the current order status.

#### Scenario: First payment succeeds
- **WHEN** a user pays an order currently in `PENDING_PAYMENT`
- **THEN** stock is deducted atomically, the coupon is marked used if present, and the order becomes `PAID`

#### Scenario: Duplicate payment is idempotent
- **WHEN** a user retries payment for an order already `PAID`, `ACCEPTED`, or `COMPLETED`
- **THEN** the system returns the successful payment status without deducting stock or using coupons again

#### Scenario: Concurrent payment wins once
- **WHEN** two payment confirmations race for the same pending order
- **THEN** only one confirmation changes the order to paid and only one stock/coupon mutation is applied

### Requirement: Atomic Coupon Claim And Reservation
The system SHALL protect coupon inventory and user coupon usage with database-level conditional updates and a `RESERVED` state.

#### Scenario: Coupon claim limit respected
- **WHEN** concurrent users claim a limited coupon
- **THEN** `claimed_count` never exceeds `total_limit`

#### Scenario: Coupon reserved for pending order
- **WHEN** a user creates an order with an unused coupon
- **THEN** the user coupon status becomes `RESERVED` and stores the pending order id

#### Scenario: Reserved coupon released
- **WHEN** an unpaid order is canceled or payment fails permanently
- **THEN** the reserved coupon returns to `UNUSED` and clears the order id

#### Scenario: Reserved coupon used
- **WHEN** payment succeeds for the order that reserved the coupon
- **THEN** the coupon becomes `USED` only if its `order_id` matches that order

### Requirement: Recoverable Transaction Failures
The system MUST leave a consistent, inspectable state when stock deduction, coupon usage, or order state updates fail.

#### Scenario: Stock unavailable after payment attempt
- **WHEN** payment confirmation finds insufficient product stock
- **THEN** the payment/order flow records a failed or unpaid result and MUST NOT mark the order `PAID`

#### Scenario: Coupon reservation conflict
- **WHEN** a coupon is already reserved by another order
- **THEN** a new order creation using that coupon fails before creating an unusable paid order
