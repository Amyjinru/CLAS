# Design

## Approach

`OrderTimeoutService` scans `PENDING_PAYMENT` orders older than `app.order-timeout.pending-payment-minutes` and moves them to `CANCELED` using conditional status updates. It releases reserved coupons and creates an order-status notification.

The scheduler runs with `@Scheduled`; tests disable automatic scanning and call the service directly.

## Configuration

- `app.order-timeout.enabled`: defaults to `true`
- `app.order-timeout.pending-payment-minutes`: defaults to `30`
- `app.order-timeout.scan-delay-ms`: defaults to `60000`

## Validation

Integration tests create a pending order, backdate `create_time`, run the timeout service, then assert:

- order status becomes `CANCELED`
- payment status maps to `FAILED`
- user receives an order-timeout notification
