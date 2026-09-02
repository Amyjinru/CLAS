# Order fulfilment E2E evidence — 2026-09-02

## Result

- `mvn --batch-mode -q clean test` (all service modules): passed.
- Gateway smoke (`services/scripts/smoke-main-path.ps1`): passed after recovery.
- Normal fulfilment regression: passed through `http://127.0.0.1:8080`.
- Controlled Catalog outage regression: passed; `POST /api/order/create` returned `503` in 35 ms and included `X-Request-Id`.

## Covered paths

- Create order with IAM address-derived delivery snapshot plus merchant and product display snapshots.
- Repeat the same `Idempotency-Key`: reuse the same order and persisted snapshots.
- User attempting merchant transition: `403`.
- Payment, refund request, merchant approval, and lifecycle timeline.
- Anonymous order creation: `401`.
- Catalog unavailable with all requests still entering through Nginx: `503` within the 5-second contract limit; Catalog was then restarted and gateway smoke passed.

## Reproducible artifacts

- [Normal regression JSON](order-e2e-regression-final-20260902.json)
- [Fault preparation JSON](order-e2e-prepare-fault-final-20260902.json)
- [Catalog-unavailable JSON](order-e2e-catalog-unavailable-final-20260902.json)

The JSON artifacts redact tokens and passwords. Run `pwsh services/scripts/run-shared-integration.ps1 -OrderE2E` on an exclusive local Docker environment, or run the normal/prepare/fault commands in `docs/订单履约端到端回归.md` when other services are already running.
