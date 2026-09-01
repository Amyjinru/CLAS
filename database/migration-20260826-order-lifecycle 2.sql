-- 配送订单闭环：订单状态变更的不可变留痕。
CREATE TABLE IF NOT EXISTS order_lifecycle_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    from_delivery_status VARCHAR(40),
    to_delivery_status VARCHAR(40),
    actor_role VARCHAR(20) NOT NULL,
    actor_id VARCHAR(30),
    remark VARCHAR(500),
    created_at DATETIME NOT NULL,
    INDEX idx_order_lifecycle_event_order_time (order_id, created_at, id),
    INDEX idx_order_lifecycle_event_type_time (event_type, created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
