# Data Integrity Governance

## Database-Enforced Relationships

These relationships have clear child lifecycles and are protected by foreign keys:

- `order_item.order_id -> orders.id`: order items cannot exist without their order.
- `order_item.product_id -> product.id`: order items must reference a real product snapshot source.
- `payment.order_id -> orders.id`: a payment has no meaning without its order.
- `review.order_id -> orders.id`: reviews are created for completed orders.
- `user_coupon.coupon_id -> coupon.id`: claimed coupons must reference an existing coupon definition.

Before enabling these constraints on an existing database, run `database/orphan-detection-core.sql`. The migration `database/migration-20260617-data-integrity.sql` removes rows whose parent is missing, then adds the constraints.

## Deferred Relationships

The following relationships remain application-governed for now:

- `orders.user_id`, `orders.merchant_id`: historical order records should survive account or merchant lifecycle changes. Owner: `OrderService`. Invariant: order creation validates the authenticated user and merchant, while deletion of historical parents must be soft or blocked.
- `product.merchant_id`: products may be hidden or archived with merchant lifecycle rules. Owner: `ProductService` and `MerchantService`. Invariant: merchant-scoped operations derive merchant identity from authentication.
- Review child tables (`review_image`, `review_reply`, `review_vote`, `review_user_hidden`): review deletion currently performs explicit backup and cleanup. Owner: `ReviewService`. Invariant: deletes must call the service cleanup path, not direct mapper deletes.
- Coupon reservations (`user_coupon.order_id`): canceled and failed payments release reservations, while used coupons preserve their order reference. Owner: `CouponService` and `PaymentService`. Invariant: only reserved coupons can be used or released for a matching order.

When one of these lifecycles becomes stable, add orphan detection first, then promote it to a database constraint with tests.
