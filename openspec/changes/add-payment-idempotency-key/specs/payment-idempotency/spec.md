# Payment Idempotency Specification

## ADDED Requirements

### Requirement: Payment requests support idempotency keys

The system SHALL allow clients to send an optional payment idempotency key for mock payment requests.

#### Scenario: Client sends payment idempotency key header

- **WHEN** a user calls `POST /api/payment/mock` with `Idempotency-Key`
- **THEN** the payment service stores the key on the created payment record
- **AND** the response includes the same `idempotencyKey`

### Requirement: Repeated payment submissions reuse the existing payment

The system SHALL return the existing payment record when the same user repeats the same idempotency key for the same order.

#### Scenario: Same key and same order

- **GIVEN** a user has successfully paid an order with an idempotency key
- **WHEN** the user submits the same payment request again with the same key
- **THEN** the response has the same `paymentId`
- **AND** no duplicate stock deduction is performed

#### Scenario: Same key and different order

- **GIVEN** a user has used an idempotency key for one order
- **WHEN** the user submits a payment request for another order with the same key
- **THEN** the request is rejected as a business error
