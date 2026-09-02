-- 订单创建幂等键：同一用户重试同一个创建请求时复用既有订单。
-- NULL 保持向后兼容；只有客户端显式发送 Idempotency-Key 才参与唯一约束。

USE clas;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_order_request_key_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'orders'
          AND COLUMN_NAME = 'client_request_key'
    ) THEN
        ALTER TABLE orders ADD COLUMN client_request_key VARCHAR(128) NULL AFTER refund_reject_reason;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'orders'
          AND INDEX_NAME = 'uk_orders_user_client_request'
    ) THEN
        ALTER TABLE orders ADD UNIQUE KEY uk_orders_user_client_request (user_id, client_request_key);
    END IF;
END //
DELIMITER ;

CALL add_order_request_key_if_missing();
DROP PROCEDURE IF EXISTS add_order_request_key_if_missing;
