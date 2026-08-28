## Context

The cart API currently returns product and merchant identifiers but no merchant name. `CartView` previews and creates an order for the first merchant, while `OrderService` consumes every cart item belonging to that merchant. Payment and payment-status APIs accept one order at a time. The order model remains correctly scoped to one merchant, so cross-merchant checkout must create multiple orders rather than weakening that invariant.

## Goals / Non-Goals

**Goals:**
- Allow explicit item and merchant selection across multiple merchants.
- Preserve per-merchant delivery, minimum-order, and coupon calculations.
- Atomically create all selected merchant orders and immediately open payment.
- Provide retry-safe aggregate payment without changing existing single-order callers.
- Reuse one receiving-status predicate in the order page and personal center.

**Non-Goals:**
- Persist cart selection across reloads.
- Introduce a real payment provider or a new payment-batch database table.
- Combine products from different merchants into one order.

## Decisions

### Selected products are explicit API input

`CreateOrderRequest` gains nullable `productIds`. Null preserves the legacy behavior of consuming all cart items for the merchant; an empty or mismatched explicit list is rejected. Preview accepts the same optional filter, so backend totals remain authoritative.

### Batch creation wraps reusable single-merchant creation

A batch request contains common address and remark plus merchant groups with product IDs and an optional coupon ID. `OrderService.createBatch` validates duplicate merchants/products and invokes an internal create method inside one Spring transaction. No checked-out cart rows are deleted permanently unless every order succeeds.

### Aggregate payment is an API composition, not a persisted aggregate

The payment page identifies the batch by order IDs returned from creation. Batch status verifies every order belongs to the authenticated user and returns individual statuses plus a total. Batch payment processes only payable orders, derives a per-order idempotency key from the batch key, and returns individual outcomes. Existing order-level conditional updates remain the source of truth, so retries cannot duplicate stock or coupon mutation. A partial outcome remains visible and only unpaid orders are retried.

### Merchant names are resolved by the cart service

The cart response includes `merchantName`, loaded in bulk with the product data. This avoids N frontend requests and gives the grouping UI a stable display label.

### Receiving classification is centralized in the frontend

A shared utility accepts an order and returns true only for `PAID` or `ACCEPTED`, not delivered, not canceled/rejected/refunded, and without an active refund. `OrdersView` and `ProfileView` both use it; unused deal vouchers retain their existing personal-center count.

### Checkout reuses the canonical location selector

Checkout initializes from the current location store before falling back to the default saved address. Editing a temporary delivery location opens the same `LocationSelector` used by profile addresses, including automatic geolocation, province/city/district selection, detailed street input, and geocoding. Temporary address coordinates are submitted and validated like saved-address coordinates so previews and order creation use the actual destination. Contact name and phone remain editable order snapshots and are mandatory before order creation or payment.

## Risks / Trade-offs

- [Risk] Frequent selection changes can issue stale preview requests. → Track a per-merchant request sequence and ignore late responses.
- [Risk] Aggregate payment can be partially successful if an order changes concurrently. → Return order-level results and retry only pending orders; rely on existing conditional payment transitions.
- [Risk] A coupon becomes invalid after selection. → Recalculate server-side and clear the selected coupon when it is absent from the latest available-coupon list.
- [Risk] Existing constructors and API callers break after DTO expansion. → Preserve convenience constructors and nullable filter behavior.

## Migration Plan

Deploy backend and frontend together. Existing single-order endpoints remain compatible, so rollback requires only reverting the new frontend route and batch endpoints; no schema rollback is needed.

## Open Questions

None. Product decisions are fixed by the approved implementation plan.
