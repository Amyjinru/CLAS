USE clas;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NULL,
    merchant_id BIGINT NOT NULL,
    user_id VARCHAR(11) NOT NULL,
    sender_role VARCHAR(10) NOT NULL COMMENT 'USER or MERCHANT',
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_merchant_user (merchant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
