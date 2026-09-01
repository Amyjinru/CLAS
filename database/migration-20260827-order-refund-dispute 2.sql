-- Order refund dispute workflow and the 15-minute post-delivery settlement hold.
USE clas;

CREATE TABLE IF NOT EXISTS order_refund_dispute (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    rider_id VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    user_reason VARCHAR(500) NOT NULL,
    merchant_reject_reason VARCHAR(255),
    original_order_status VARCHAR(20) NOT NULL,
    original_delivery_status VARCHAR(40),
    admin_reason VARCHAR(500),
    reviewer_id VARCHAR(20),
    created_at DATETIME NOT NULL,
    reviewed_at DATETIME,
    INDEX idx_order_refund_dispute_status (status, created_at DESC),
    INDEX idx_order_refund_dispute_order (order_id, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
