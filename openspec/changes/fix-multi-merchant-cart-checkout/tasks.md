## 1. Backend checkout contracts

- [x] 1.1 Enrich cart items with merchant names using batched merchant loading
- [x] 1.2 Add optional selected product IDs to single-merchant preview and creation while preserving legacy behavior
- [x] 1.3 Add validated atomic multi-merchant order creation request and response APIs

## 2. Aggregate payment

- [x] 2.1 Add authenticated aggregate payment status and payment request/response contracts
- [x] 2.2 Implement retry-safe aggregate payment over existing order-level idempotent transitions

## 3. Frontend experience

- [x] 3.1 Rebuild the cart as merchant groups with unchecked item and tri-state merchant selection
- [x] 3.2 Add per-merchant previews and coupons, selected totals, validation, and batch order submission
- [x] 3.3 Add the aggregate payment route and page behavior while preserving single-order and deal payments
- [x] 3.4 Centralize paid-undelivered classification and use it in orders and personal-center counts

## 4. Verification

- [x] 4.1 Add backend integration coverage for selected products, atomic batch rollback, and aggregate payment retries
- [x] 4.2 Add frontend unit-testable selection/status helpers and focused tests where supported
- [x] 4.3 Run backend tests and frontend production build, fixing regressions

## 5. Runtime repair

- [x] 5.1 Remove byte-identical untracked “ 2” source copies that caused duplicate Java class compilation
- [x] 5.2 Align the default Maven bytecode target with Spring Boot 3.3 runtime support
- [x] 5.3 Re-run standard backend tests, frontend tests/build, and a real application startup smoke check

## 6. Local browser and CI verification

- [x] 6.1 Reproduce and fix the GitHub Actions Java toolchain mismatch
- [x] 6.2 Start the backend and frontend locally and verify the changed user flows in a real browser
