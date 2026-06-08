CREATE DATABASE IF NOT EXISTS clas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clas;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing $$
CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns c
        WHERE c.table_schema = DATABASE()
          AND c.table_name = table_name_value
          AND c.column_name = column_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN ', column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_column_if_missing('user', 'enabled', 'enabled TINYINT(1) NOT NULL DEFAULT 1');

CALL add_column_if_missing('merchant', 'user_id', 'user_id VARCHAR(20)');
CALL add_column_if_missing('merchant', 'phone', 'phone VARCHAR(20)');
CALL add_column_if_missing('merchant', 'category', 'category VARCHAR(50)');
CALL add_column_if_missing('merchant', 'address', 'address VARCHAR(255)');
CALL add_column_if_missing('merchant', 'business_hours', 'business_hours VARCHAR(100)');
CALL add_column_if_missing('merchant', 'delivery_fee', 'delivery_fee INT NOT NULL DEFAULT 0');
CALL add_column_if_missing('merchant', 'min_order_price', 'min_order_price INT NOT NULL DEFAULT 0');
CALL add_column_if_missing('merchant', 'average_price', 'average_price INT NOT NULL DEFAULT 0');
CALL add_column_if_missing('merchant', 'score', 'score DECIMAL(3,2) DEFAULT 0.00');
CALL add_column_if_missing('merchant', 'status', 'status VARCHAR(20) NOT NULL DEFAULT ''OPEN''');
CALL add_column_if_missing('merchant', 'bank_account', 'bank_account VARCHAR(50)');
CALL add_column_if_missing('merchant', 'admin_remarks', 'admin_remarks VARCHAR(255)');
CALL add_column_if_missing('merchant', 'settlement_cycle', 'settlement_cycle INT');
CALL add_column_if_missing('merchant', 'created_at', 'created_at DATETIME');
CALL add_column_if_missing('merchant', 'updated_at', 'updated_at DATETIME');
UPDATE merchant
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW()),
    business_hours = COALESCE(business_hours, '09:00-21:00'),
    delivery_fee = COALESCE(delivery_fee, 0),
    min_order_price = COALESCE(min_order_price, 0),
    average_price = COALESCE(average_price, 0),
    score = COALESCE(score, 0.00),
    status = COALESCE(status, 'OPEN');

UPDATE merchant
SET user_id = COALESCE(user_id, '13800000002'),
    phone = COALESCE(phone, '13800000022'),
    category = COALESCE(category, '美食'),
    address = COALESCE(address, '软件园东门 1 号'),
    bank_account = COALESCE(bank_account, '6222000000000000001'),
    settlement_cycle = COALESCE(settlement_cycle, 7)
WHERE id = 1;

INSERT INTO merchant (id, user_id, merchant_name, phone, category, address, business_hours, delivery_fee, min_order_price, average_price, score, status, bank_account, settlement_cycle, created_at, updated_at)
SELECT 1, '13800000002', '校园轻食铺', '13800000022', '美食', '软件园东门 1 号', '09:00-21:00', 300, 1500, 2800, 4.70, 'OPEN', '6222000000000000001', 7, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE id = 1);

CALL add_column_if_missing('product', 'image', 'image VARCHAR(255)');
CALL add_column_if_missing('product', 'status', 'status VARCHAR(20) NOT NULL DEFAULT ''ON_SALE''');
CALL add_column_if_missing('product', 'created_at', 'created_at DATETIME');
CALL add_column_if_missing('product', 'updated_at', 'updated_at DATETIME');
UPDATE product
SET status = COALESCE(status, 'ON_SALE'),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

CALL add_column_if_missing('orders', 'merchant_id', 'merchant_id BIGINT');
CALL add_column_if_missing('orders', 'total_price', 'total_price INT NOT NULL DEFAULT 0');
CALL add_column_if_missing('orders', 'delivery_address', 'delivery_address VARCHAR(255)');
CALL add_column_if_missing('orders', 'delivery_status', 'delivery_status VARCHAR(20) NOT NULL DEFAULT ''WAITING''');
CALL add_column_if_missing('orders', 'estimated_minutes', 'estimated_minutes INT NOT NULL DEFAULT 30');
CALL add_column_if_missing('orders', 'refund_reason', 'refund_reason VARCHAR(255)');
CALL add_column_if_missing('orders', 'refund_status', 'refund_status VARCHAR(20) NOT NULL DEFAULT ''NONE''');

CALL add_column_if_missing('review', 'merchant_reply', 'merchant_reply TEXT');
CALL add_column_if_missing('review', 'report_reason', 'report_reason VARCHAR(255)');
CALL add_column_if_missing('review', 'report_status', 'report_status VARCHAR(20) NOT NULL DEFAULT ''NONE''');

CALL add_column_if_missing('payment', 'pay_method', 'pay_method VARCHAR(20) NOT NULL DEFAULT ''MOCK''');

CREATE TABLE IF NOT EXISTS merchant_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    admin_id VARCHAR(20) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    created_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_favorite_user_merchant (user_id, merchant_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    read_flag TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS group_deal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    original_price INT NOT NULL,
    deal_price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    valid_days INT NOT NULL DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deal_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    voucher_code VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    pay_amount INT NOT NULL,
    create_time DATETIME NOT NULL,
    used_time DATETIME
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS service_booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    appointment_time DATETIME NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `user` (phone, username, password, role, enabled) VALUES
    ('13800000001', 'user', 'Abc123!', 'USER', 1),
    ('13800000002', 'merchant', 'Abc123!', 'MERCHANT', 1),
    ('13800000003', 'admin', 'Abc123!', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password = VALUES(password),
    role = VALUES(role),
    enabled = VALUES(enabled);

DROP PROCEDURE IF EXISTS add_column_if_missing;
