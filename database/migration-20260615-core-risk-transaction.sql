-- Core risk transaction consistency migration
-- Introduces the RESERVED user_coupon state used while an order is pending payment.

CREATE INDEX idx_user_coupon_order_status ON user_coupon (order_id, status);

-- Preserve existing pending-order reservations created before the RESERVED state.
UPDATE user_coupon uc
JOIN orders o ON o.id = uc.order_id
SET uc.status = 'RESERVED'
WHERE uc.status = 'UNUSED'
  AND o.status = 'PENDING_PAYMENT';
