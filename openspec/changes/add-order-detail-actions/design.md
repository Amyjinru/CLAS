# Add Order Detail Actions Design

## Approach

Reuse the action rules from `OrdersView`:

- `PENDING_PAYMENT`: show payment and cancel.
- `PAID`: show cancel and refund.
- `ACCEPTED`: show complete and refund.
- `COMPLETED`: show refund and review link.

After cancel, complete, or refund, reload the current order detail by ID so the page reflects the latest status and timeline.
