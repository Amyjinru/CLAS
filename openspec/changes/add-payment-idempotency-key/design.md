# Design

## Approach

Payment requests accept an optional `Idempotency-Key` header. The same value can also be passed in the JSON request body as `idempotencyKey`, with the header taking precedence.

`PaymentService` normalizes the key, checks whether the current user already has a payment record with that key, and returns the existing record when it belongs to the same order. If the key belongs to another order, the service rejects the request.

The `payment` table stores `idempotency_key` and a unique `(user_id, idempotency_key)` index. Nullable keys preserve legacy behavior.

## Compatibility

Existing clients that omit the key keep the current behavior. Existing payment response fields remain unchanged, with `idempotencyKey` added as optional metadata.

## Validation

Integration tests cover repeated successful payment with the same key and assert the same `paymentId` is returned.
