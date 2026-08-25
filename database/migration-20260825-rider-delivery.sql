-- Rider delivery foundation: additive, repeatable migration for CLAS MySQL.
USE clas;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_column_if_missing(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_definition);
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role),
    INDEX idx_user_role_status (role, status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO user_role (user_id, role, status, created_at, updated_at)
SELECT phone, role, CASE WHEN enabled = 1 THEN 'APPROVED' ELSE 'DISABLED' END, NOW(), NOW()
FROM `user`
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

CREATE TABLE IF NOT EXISTS rider_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    id_card_ciphertext VARCHAR(1024) NOT NULL,
    id_card_masked VARCHAR(32) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    service_area VARCHAR(100) NOT NULL,
    emergency_contact_name VARCHAR(50) NOT NULL,
    emergency_contact_phone VARCHAR(20) NOT NULL,
    credential_urls TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(255),
    reviewer_id VARCHAR(20),
    reviewed_at DATETIME,
    created_at DATETIME NOT NULL,
    INDEX idx_rider_application_user (user_id, created_at DESC),
    INDEX idx_rider_application_status (status, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rider_profile (
    user_id VARCHAR(20) PRIMARY KEY,
    real_name VARCHAR(50) NOT NULL,
    id_card_ciphertext VARCHAR(1024) NOT NULL,
    id_card_masked VARCHAR(32) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    service_area VARCHAR(100) NOT NULL,
    emergency_contact_name VARCHAR(50) NOT NULL,
    emergency_contact_phone VARCHAR(20) NOT NULL,
    online_status TINYINT(1) NOT NULL DEFAULT 0,
    accepting_orders TINYINT(1) NOT NULL DEFAULT 0,
    max_active_orders INT NOT NULL DEFAULT 3,
    current_longitude DECIMAL(10,6),
    current_latitude DECIMAL(10,6),
    location_updated_at DATETIME,
    withdrawable_balance INT NOT NULL DEFAULT 0,
    frozen_balance INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_rider_profile_online (status, online_status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rider_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rider_id VARCHAR(20) NOT NULL,
    operator_id VARCHAR(20) NOT NULL,
    action VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    before_value TEXT,
    after_value TEXT,
    created_at DATETIME NOT NULL,
    INDEX idx_rider_audit_rider (rider_id, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rider_location_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rider_id VARCHAR(20) NOT NULL,
    longitude DECIMAL(10,6) NOT NULL,
    latitude DECIMAL(10,6) NOT NULL,
    accuracy_meters INT,
    reported_at DATETIME NOT NULL,
    INDEX idx_rider_location_history (rider_id, reported_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CALL add_column_if_missing('merchant', 'default_prepare_minutes', 'default_prepare_minutes INT NOT NULL DEFAULT 15 AFTER business_hours');
CALL add_column_if_missing('orders', 'rider_id', 'rider_id VARCHAR(20) NULL AFTER merchant_id');
CALL add_column_if_missing('rider_profile', 'accepting_orders', 'accepting_orders TINYINT(1) NOT NULL DEFAULT 0 AFTER online_status');
ALTER TABLE orders MODIFY COLUMN delivery_status VARCHAR(40) NOT NULL DEFAULT 'WAITING';
CALL add_column_if_missing('orders', 'rider_assigned_at', 'rider_assigned_at DATETIME NULL AFTER accepted_at');
CALL add_column_if_missing('orders', 'picked_up_at', 'picked_up_at DATETIME NULL AFTER rider_assigned_at');
CALL add_column_if_missing('orders', 'delivery_completed_at', 'delivery_completed_at DATETIME NULL AFTER picked_up_at');
CALL add_column_if_missing('orders', 'prepare_minutes_snapshot', 'prepare_minutes_snapshot INT NULL AFTER estimated_minutes');
CALL add_column_if_missing('orders', 'promise_start_at', 'promise_start_at DATETIME NULL AFTER prepare_minutes_snapshot');
CALL add_column_if_missing('orders', 'promise_end_at', 'promise_end_at DATETIME NULL AFTER promise_start_at');
CALL add_column_if_missing('orders', 'predicted_arrival_at', 'predicted_arrival_at DATETIME NULL AFTER promise_end_at');
CALL add_column_if_missing('orders', 'rider_commission', 'rider_commission INT NOT NULL DEFAULT 0 AFTER delivery_fee');
CALL add_column_if_missing('orders', 'reassign_count', 'reassign_count INT NOT NULL DEFAULT 0 AFTER rider_commission');
CALL add_column_if_missing('orders', 'delivery_sequence', 'delivery_sequence INT NULL AFTER reassign_count');
CREATE INDEX idx_orders_rider_delivery ON orders (rider_id, delivery_status, create_time);

CREATE TABLE IF NOT EXISTS delivery_exception (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    rider_id VARCHAR(20),
    exception_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    score_deduction INT NOT NULL DEFAULT 0,
    commission_deduction INT NOT NULL DEFAULT 0,
    detail VARCHAR(255),
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_delivery_exception (order_id, exception_type),
    INDEX idx_delivery_exception_status (status, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rider_settlement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rider_id VARCHAR(20) NOT NULL,
    order_id BIGINT,
    source_type VARCHAR(30) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    settlement_type VARCHAR(30) NOT NULL,
    amount INT NOT NULL,
    balance_type VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_rider_settlement_source (source_type, source_id),
    INDEX idx_rider_settlement_rider (rider_id, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rider_withdrawal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rider_id VARCHAR(20) NOT NULL,
    bank_card_id BIGINT NOT NULL,
    amount INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id VARCHAR(20),
    review_reason VARCHAR(255),
    created_at DATETIME NOT NULL,
    reviewed_at DATETIME,
    INDEX idx_rider_withdrawal_rider (rider_id, created_at DESC),
    INDEX idx_rider_withdrawal_status (status, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rider_tip (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    rider_id VARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PAID',
    paid_at DATETIME NOT NULL,
    UNIQUE KEY uk_rider_tip_order (order_id),
    UNIQUE KEY uk_rider_tip_idempotency (user_id, idempotency_key)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rider_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    rider_id VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    tags VARCHAR(255),
    content VARCHAR(500),
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_rider_review_order (order_id),
    INDEX idx_rider_review_rider (rider_id, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rider_daily_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rider_id VARCHAR(20) NOT NULL,
    metric_date DATE NOT NULL,
    completed_orders INT NOT NULL DEFAULT 0,
    net_income INT NOT NULL DEFAULT 0,
    average_rating DECIMAL(3,2),
    overdue_count INT NOT NULL DEFAULT 0,
    average_delivery_minutes INT NOT NULL DEFAULT 0,
    base_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    manual_adjustment DECIMAL(5,2) NOT NULL DEFAULT 0,
    final_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    grade VARCHAR(2) NOT NULL DEFAULT 'D',
    archived_at DATETIME,
    UNIQUE KEY uk_rider_daily_metrics (rider_id, metric_date)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS delivery_call_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    rider_id VARCHAR(20) NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    masked_phone VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_call_session_order (order_id, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    conversation_type VARCHAR(30) NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    peer_id VARCHAR(20) NOT NULL,
    last_message_at DATETIME,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_chat_conversation (order_id, conversation_type),
    INDEX idx_chat_conversation_user (user_id, last_message_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Preserve USER_MERCHANT history and add an order-scoped USER_RIDER channel.
CALL add_column_if_missing('chat_message', 'conversation_type', "conversation_type VARCHAR(30) NOT NULL DEFAULT 'USER_MERCHANT' AFTER order_id");
CALL add_column_if_missing('chat_message', 'rider_id', 'rider_id VARCHAR(20) NULL AFTER user_id');
ALTER TABLE chat_message MODIFY COLUMN merchant_id BIGINT NULL;
CREATE INDEX idx_chat_message_rider_order ON chat_message (order_id, conversation_type, rider_id, created_at);

DROP PROCEDURE IF EXISTS add_column_if_missing;
