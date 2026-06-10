## 1. Strict Bearer Authentication

- [x] 1.1 Remove `Authorization: <phone>` fallback from `AuthInterceptor` and ensure non-Bearer values cannot authenticate protected APIs.
- [x] 1.2 Add JWT secret startup validation that rejects missing, known default, or too-short secrets outside the test profile.
- [x] 1.3 Update test configuration to provide a test-only JWT secret and keep integration tests deterministic.
- [x] 1.4 Remove frontend phone-header fallback from `frontend/src/api/client.js` and ensure only stored JWT tokens produce authorization headers.
- [x] 1.5 Add backend regression tests for valid Bearer token, missing token, and direct phone authorization rejection.

## 2. Payment And Coupon Consistency

- [x] 2.1 Add mapper methods for conditional order status updates, conditional payment status updates, and successful-payment lookup by order.
- [x] 2.2 Refactor mock payment so simulated delay occurs outside database transactions and payment confirmation uses short transactional steps.
- [x] 2.3 Make payment confirmation idempotent for already-paid orders and safe under concurrent retries.
- [x] 2.4 Add coupon `RESERVED` state support in schema/test schema/migration and entity/service constants.
- [x] 2.5 Add atomic coupon claim update that prevents `claimed_count` from exceeding `total_limit`.
- [x] 2.6 Change order creation to reserve coupons with `status = UNUSED` and matching `user_id` conditions.
- [x] 2.7 Change payment success, cancellation, and failure paths to use/release reserved coupons by matching `order_id`.
- [x] 2.8 Add integration tests for duplicate payment, insufficient stock, coupon claim limit, coupon reservation, coupon release, and coupon use.

## 3. Query Performance

- [x] 3.1 Add mapper aggregation queries/DTO mapping for dashboard totals and date-ranged order stats.
- [x] 3.2 Replace sales overview, merchant ranking, and top product calculations with SQL `GROUP BY`, `ORDER BY`, and `LIMIT` queries.
- [x] 3.3 Refactor `OrderService.withItems` to batch-load order items for all returned orders.
- [x] 3.4 Refactor review list/detail assembly to batch-load users, images, replies, and votes.
- [x] 3.5 Add or update indexes for order stats, merchant ranking, product ranking, order item lookup, and review detail lookup.
- [x] 3.6 Add tests or query-shape checks that guard against reintroducing obvious N+1 behavior in order and review listing.

## 4. Data Integrity Governance

- [x] 4.1 Add orphan detection SQL for core relationships before enabling new constraints.
- [x] 4.2 Add migration steps for safe foreign keys where lifecycle rules are clear: order item to order/product, payment to order, review to order, user coupon to coupon.
- [x] 4.3 Document deferred relationships that cannot yet use foreign keys, including invariant owner service and cleanup strategy.
- [x] 4.4 Update `database/schema.sql` and `backend/src/test/resources/schema-test.sql` to match the accepted constraint/index model.
- [x] 4.5 Add integration tests for rejected orphan inserts or service-level guards where database foreign keys are deferred.

## 5. API Surface Governance

- [x] 5.1 Add canonical current-user cart APIs that derive user id from `UserContext` and do not accept `{userId}`.
- [x] 5.2 Add canonical current-user order APIs that derive user id from `UserContext` and do not accept `{userId}`.
- [x] 5.3 Add canonical current-merchant APIs for own orders/products where client-provided merchant ids are currently ignored.
- [x] 5.4 Mark legacy id-bearing routes deprecated and delegate them to the canonical authenticated implementations during the compatibility window.
- [x] 5.5 Update frontend API wrappers to call canonical routes and stop sending current user id in request bodies for current-user operations.
- [x] 5.6 Add controller tests proving client-supplied ids cannot access or mutate another user's or merchant's data.

## 6. Frontend Maintainability And Build Size

- [x] 6.1 Add shared frontend formatters for fen-to-yuan money, date/time, and common status label/type maps.
- [x] 6.2 Add reusable `MoneyText` and `StatusTag` components and migrate high-traffic pages to use them.
- [x] 6.3 Add composables for table query/pagination and confirmation actions, then migrate admin list pages incrementally.
- [ ] 6.4 Split `ProfileView.vue` into focused profile, address, penalty/appeal, and account-action sections.
- [ ] 6.5 Split `MerchantProductsView.vue` into category management, product table, product form, image upload, and status action components.
- [ ] 6.6 Extract reusable cart/order action UI from `CartView.vue` and `MerchantDetailView.vue` without changing user-visible flows.
- [x] 6.7 Configure Vite manual chunks for Vue, Element Plus, ECharts/charts, and app code.
- [x] 6.8 Change chart imports/routes so ECharts code loads only for dashboard or analytics views.

## 7. Verification

- [x] 7.1 Run `mvn test` and fix any backend regressions.
- [x] 7.2 Run `npm run build` and confirm production build succeeds.
- [x] 7.3 Verify the build output no longer reports avoidable oversized chart initial chunks, or document remaining chunk-size rationale.
- [ ] 7.4 Perform manual smoke checks for login/session expiry, cart checkout, payment, coupon use, admin dashboard, merchant products, and profile center.
- [x] 7.5 Update docs or release notes for breaking authentication/config changes and legacy API deprecation.
