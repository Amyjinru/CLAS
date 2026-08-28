# Add Pending Payment Timeout

## Why

The transaction reliability roadmap calls for a pending-payment timeout before adopting heavier MQ infrastructure. CLAS currently leaves unpaid orders in `PENDING_PAYMENT` indefinitely unless the user cancels manually.

## What Changes

- Add a scheduled service that cancels pending-payment orders older than the configured timeout.
- Release reserved coupons when an unpaid order expires.
- Notify the user that the order was automatically canceled.
- Keep the implementation lightweight with Spring scheduling and MySQL.

## Non-Goals

- No RocketMQ/delayed-message dependency in this slice.
- No frontend countdown UI in this slice.
