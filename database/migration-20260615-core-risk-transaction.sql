-- Core risk transaction consistency migration
-- Introduces the RESERVED user_coupon state used while an order is pending payment.

USE clas;

-- 幂等：仅在索引不存在时才创建
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'clas' AND TABLE_NAME = 'user_coupon'
                   AND INDEX_NAME = 'idx_user_coupon_order_status');

SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_user_coupon_order_status ON user_coupon (order_id, status)',
    'SELECT "Index idx_user_coupon_order_status already exists, skipping."');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Preserve existing pending-order reservations created before the RESERVED state.
UPDATE user_coupon uc
JOIN orders o ON o.id = uc.order_id
SET uc.status = 'RESERVED'
WHERE uc.status = 'UNUSED'
  AND o.status = 'PENDING_PAYMENT';
