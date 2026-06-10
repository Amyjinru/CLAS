# Core Risk Remediation Notes

## Authentication

- Protected APIs now accept only `Authorization: Bearer <jwt>`.
- The legacy `Authorization: <phone>` compatibility path has been removed and returns `401`.
- Non-test runtime must provide `JWT_SECRET`. Missing, known development defaults, or secrets shorter than 32 characters are rejected at startup.

## Payment And Coupons

- Mock payment delay now happens outside database transactions.
- Payment confirmation uses conditional order status transitions and is idempotent for already-paid orders.
- Coupons now use `UNUSED -> RESERVED -> USED` for order reservation and payment success. Failed or canceled orders release reservations.

## API Compatibility Window

Canonical authenticated routes were added:

- `GET /api/cart/me`
- `DELETE /api/cart/me/items/{productId}`
- `GET /api/cart/me/validation`
- `DELETE /api/cart/me/invalid`
- `DELETE /api/cart/me`
- `GET /api/order/me`
- `GET /api/order/merchant/me`
- `/api/merchant/me/products...`

Legacy id-bearing routes remain during the compatibility window and delegate to authenticated `UserContext` or current merchant identity. Clients should migrate to the canonical routes and stop sending current user ids in request bodies.

## Database

- Run `database/orphan-detection-core.sql` before adding foreign keys on existing data.
- Apply `database/migration-20260616-query-performance.sql` for performance indexes.
- Apply `database/migration-20260617-data-integrity.sql` to clean orphan rows and add core foreign keys.

## Frontend Build

- ECharts is now dynamically imported only by dashboard/analytics views and registered through `echarts/core`.
- The chart chunk is no longer part of normal route initial loading. It remains slightly above Vite's default 500 kB warning threshold because chart rendering still needs line, bar, pie, grid, legend, tooltip, and canvas renderer modules.
- Element Plus remains a large independent vendor chunk. Further reduction would require on-demand Element Plus component imports or replacing broad UI imports.
