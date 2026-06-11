## 1. Backend

- [x] 1.1 Add payment idempotency key to request/response DTOs and entity mapping.
- [x] 1.2 Store idempotency keys in the payment table with a per-user unique index.
- [x] 1.3 Accept `Idempotency-Key` on payment endpoints.
- [x] 1.4 Reuse an existing payment when the same user repeats the same key for the same order.

## 2. Verification and Release

- [x] 2.1 Add integration coverage for repeated payment submissions.
- [x] 2.2 Run backend tests and frontend build.
- [x] 2.3 Push `dev` and deploy to the cloud server.
