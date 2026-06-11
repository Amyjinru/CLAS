# Add Order Detail API Design

## Approach

Expose role-scoped read endpoints that return the same `OrderResponse` shape as order list APIs. This keeps frontend rendering stable while avoiding full-list fetches for detail views.

## Endpoints

- `GET /api/order/{orderId}` for the current user.
- `GET /api/order/merchant/detail/{orderId}` for the current merchant.
- `GET /api/order/admin/{orderId}` for admins.

The merchant path intentionally includes `detail` to avoid conflict with the deprecated `GET /api/order/merchant/{merchantId}` list route.

## Authorization

The service layer reuses `requireUserOrder`, `requireMerchantOrder`, and `requireOrder`. This keeps the new read model aligned with the mutation authorization rules already used by cancel, accept, deliver, complete, and refund flows.
