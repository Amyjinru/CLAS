# Add Payment Idempotency Key

## Why

The Meituan-inspired transaction reliability track calls out duplicate-payment idempotency. CLAS already reuses successful payment records for paid orders, but clients cannot explicitly mark repeated payment submissions as the same operation.

## What Changes

- Add optional `Idempotency-Key` support to mock payment endpoints.
- Store payment idempotency keys per user and prevent the same key from being reused for another order.
- Return the persisted `idempotencyKey` in payment responses for easier frontend and test verification.
- Add integration coverage for repeated payment submission with the same key.

## Non-Goals

- No real third-party payment gateway integration.
- No distributed lock or message queue in this slice.
- No change to existing clients that do not send `Idempotency-Key`.
