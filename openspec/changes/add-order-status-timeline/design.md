# Add Order Status Timeline Design

## Approach

Store the first-order lifecycle timestamps directly on `orders`. This keeps the current `OrderResponse` shape compatible because the order entity already serializes as part of list and detail responses.

## Fields

- `paid_at`
- `accepted_at`
- `delivered_at`
- `completed_at`
- `canceled_at`
- `rejected_at`

Refund lifecycle timestamps already exist as `refund_requested_at` and `refund_resolved_at`.

## Rendering

The user detail page derives a timeline from the order payload and only displays nodes that have timestamps. Created time is always the first node.
