CREATE TABLE IF NOT EXISTS user_bank_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    bank_name VARCHAR(50) NOT NULL,
    cardholder_name VARCHAR(50) NOT NULL,
    card_no_encrypted VARCHAR(64) NOT NULL,
    card_last4 VARCHAR(4) NOT NULL,
    card_type VARCHAR(20) NOT NULL DEFAULT '借记卡',
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    INDEX idx_user_bank_card_user (user_id, create_time DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
