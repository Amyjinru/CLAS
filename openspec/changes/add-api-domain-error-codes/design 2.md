# Design

## Approach

`Result` gains an optional `errorCode`. Success responses set it to `null`; error responses populate it through `GlobalExceptionHandler`.

`BusinessException` can carry an explicit error code, but existing throw sites remain compatible. A centralized `DomainErrorCode` classifier derives broad domain codes from HTTP status and message patterns, giving immediate coverage without a high-risk mass rewrite.

## Compatibility

Existing frontend unwrap logic reads `response.data.data`, so an added sibling field is non-breaking. Existing tests that assert `code`, `message`, or `data` keep working.

## Validation

Integration tests verify:

- Missing group deal returns `RESOURCE_NOT_FOUND`.
- Reusing a payment idempotency key for another order returns `PAYMENT_IDEMPOTENCY_CONFLICT`.
