-- P1：优惠券 MVP、配送/退款字段、团购增强

USE clas;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_column_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    coupon_type VARCHAR(20) NOT NULL DEFAULT 'FIXED',
    discount_amount INT NOT NULL DEFAULT 0,
    discount_percent INT NULL,
    min_order_amount INT NOT NULL DEFAULT 0,
    merchant_id BIGINT NULL,
    total_limit INT NOT NULL DEFAULT 0,
    claimed_count INT NOT NULL DEFAULT 0,
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    order_id BIGINT NULL,
    claimed_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    UNIQUE KEY uk_user_coupon (user_id, coupon_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deal_redeem_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_order_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    voucher_code VARCHAR(40) NOT NULL,
    operator_id VARCHAR(20) NOT NULL,
    redeemed_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CALL add_column_if_missing('orders', 'coupon_discount', 'coupon_discount INT NOT NULL DEFAULT 0 AFTER delivery_fee');
CALL add_column_if_missing('orders', 'user_coupon_id', 'user_coupon_id BIGINT NULL AFTER coupon_discount');
CALL add_column_if_missing('orders', 'refund_reject_reason', 'refund_reject_reason VARCHAR(255) NULL AFTER reject_reason');
CALL add_column_if_missing('orders', 'refund_requested_at', 'refund_requested_at DATETIME NULL AFTER refund_status');
CALL add_column_if_missing('orders', 'refund_resolved_at', 'refund_resolved_at DATETIME NULL AFTER refund_requested_at');

CALL add_column_if_missing('deal_order', 'paid_time', 'paid_time DATETIME NULL AFTER pay_amount');
CALL add_column_if_missing('deal_order', 'expire_time', 'expire_time DATETIME NULL AFTER paid_time');

INSERT INTO coupon (title, description, coupon_type, discount_amount, min_order_amount, merchant_id, total_limit, claimed_count, valid_from, valid_to, status, created_at)
SELECT '新用户满减券', '外卖订单满 ¥20 减 ¥3', 'FIXED', 300, 2000, NULL, 1000, 0, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM coupon WHERE title = '新用户满减券');

INSERT INTO coupon (title, description, coupon_type, discount_amount, min_order_amount, merchant_id, total_limit, claimed_count, valid_from, valid_to, status, created_at)
SELECT '轻食铺专享券', '校园轻食铺满 ¥15 减 ¥2', 'FIXED', 200, 1500, 1, 500, 0, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM coupon WHERE title = '轻食铺专享券');

DROP PROCEDURE IF EXISTS add_column_if_missing;
