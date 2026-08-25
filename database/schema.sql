CREATE DATABASE IF NOT EXISTS clas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clas;

-- ============================================================
-- DROP (按外键依赖反序)
-- ============================================================
DROP TABLE IF EXISTS deal_redeem_log;
DROP TABLE IF EXISTS user_coupon;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS appeal;
DROP TABLE IF EXISTS user_penalty;
DROP TABLE IF EXISTS deleted_review_backup;
DROP TABLE IF EXISTS review_delete_request;
DROP TABLE IF EXISTS review_user_hidden;
DROP TABLE IF EXISTS review_vote;
DROP TABLE IF EXISTS review_reply;
DROP TABLE IF EXISTS review_image;
DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS service_booking;
DROP TABLE IF EXISTS deal_order;
DROP TABLE IF EXISTS group_deal;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS user_bank_card;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS product_category;
DROP TABLE IF EXISTS merchant_audit_log;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS role_application;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS `user`;

-- ============================================================
-- 核心业务表
-- ============================================================

CREATE TABLE `user` (
    phone VARCHAR(20) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    avatar VARCHAR(512),
    nickname VARCHAR(50)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    target_role VARCHAR(20) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_remarks VARCHAR(255),
    operator_id VARCHAR(20),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_role_application_user (user_id, created_at DESC),
    INDEX idx_role_application_status (target_role, status, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role),
    INDEX idx_user_role_role (role)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    logo VARCHAR(512),
    phone VARCHAR(20) NOT NULL UNIQUE,
    category VARCHAR(50),
    address VARCHAR(255),
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    delivery_radius_m INT NOT NULL DEFAULT 3000,
    business_hours VARCHAR(100),
    delivery_fee INT NOT NULL DEFAULT 0,
    min_order_price INT NOT NULL DEFAULT 0,
    average_price INT NOT NULL DEFAULT 0,
    score DECIMAL(3,2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    manual_closed TINYINT(1) NOT NULL DEFAULT 0,
    bank_account VARCHAR(50),
    admin_remarks VARCHAR(255),
    settlement_cycle INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_merchant_user_id (user_id),
    INDEX idx_merchant_status (status),
    INDEX idx_merchant_category (category)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE merchant_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    admin_id VARCHAR(20) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    created_at DATETIME NOT NULL,
    INDEX idx_audit_log_merchant (merchant_id, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_product_category_merchant_name (merchant_id, name),
    INDEX idx_product_category_merchant (merchant_id, sort_order, id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_product_merchant (merchant_id, status),
    INDEX idx_product_category_id (category_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    INDEX idx_cart_user (user_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_address_user (user_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_bank_card (
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

CREATE TABLE favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_favorite_user_merchant (user_id, merchant_id),
    INDEX idx_favorite_user (user_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    read_flag TINYINT(1) NOT NULL DEFAULT 0,
    type VARCHAR(50),
    target_type VARCHAR(50),
    target_id BIGINT,
    review_id BIGINT,
    reply_id BIGINT,
    order_id BIGINT,
    merchant_id BIGINT,
    target_path VARCHAR(255),
    created_at DATETIME NOT NULL,
    INDEX idx_notification_user_read (user_id, read_flag, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 交易表
-- ============================================================

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    total_price INT NOT NULL,
    subtotal INT NOT NULL DEFAULT 0,
    delivery_fee INT NOT NULL DEFAULT 0,
    coupon_discount INT NOT NULL DEFAULT 0,
    user_coupon_id BIGINT,
    status VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(255),
    delivery_longitude DECIMAL(10,6),
    delivery_latitude DECIMAL(10,6),
    distance_meters INT,
    route_distance_meters INT,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    rider_id VARCHAR(20),
    rider_accepted_at DATETIME,
    estimated_minutes INT NOT NULL DEFAULT 30,
    refund_reason VARCHAR(255),
    refund_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    refund_requested_at DATETIME,
    refund_resolved_at DATETIME,
    remark VARCHAR(255),
    reject_reason VARCHAR(255),
    refund_reject_reason VARCHAR(255),
    create_time DATETIME NOT NULL,
    paid_at DATETIME,
    accepted_at DATETIME,
    delivered_at DATETIME,
    completed_at DATETIME,
    canceled_at DATETIME,
    rejected_at DATETIME,
    INDEX idx_orders_user (user_id, create_time DESC),
    INDEX idx_orders_merchant_status (merchant_id, status, create_time DESC),
    INDEX idx_orders_rider (rider_id, rider_accepted_at DESC),
    INDEX idx_orders_status (status),
    INDEX idx_orders_create_status (create_time, status),
    INDEX idx_orders_merchant_create_status (merchant_id, create_time, status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price INT NOT NULL,
    INDEX idx_order_item_order (order_id),
    INDEX idx_order_item_product_order (product_id, order_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    merchant_id BIGINT NOT NULL,
    user_id VARCHAR(11) NOT NULL,
    sender_role VARCHAR(10) NOT NULL COMMENT 'USER or MERCHANT',
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_chat_order (order_id),
    INDEX idx_chat_merchant_user (merchant_id, user_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 评价体系
-- ============================================================

CREATE TABLE review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    content TEXT,
    merchant_reply TEXT,
    report_reason VARCHAR(255),
    report_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    created_at DATETIME,
    INDEX idx_review_user (user_id),
    INDEX idx_review_order (order_id),
    INDEX idx_review_order_id (order_id, id),
    INDEX idx_review_report_status (report_status),
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_review_image_review (review_id),
    INDEX idx_review_image_review_sort (review_id, sort_order)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    parent_reply_id BIGINT,
    user_id VARCHAR(20) NOT NULL,
    reply_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    content TEXT NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_review_reply_review (review_id, created_at),
    INDEX idx_review_reply_review_deleted_id (review_id, deleted, id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_vote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    vote_type VARCHAR(10) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_review_vote (target_type, target_id, user_id),
    INDEX idx_review_vote_target (target_type, target_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_user_hidden (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_review_hidden (review_id, user_id),
    INDEX idx_review_hidden_user (user_id, review_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE deleted_review_backup (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    order_id BIGINT NOT NULL,
    score INT NOT NULL,
    content TEXT,
    images_json TEXT,
    deleted_by VARCHAR(20) NOT NULL,
    delete_type VARCHAR(20) NOT NULL,
    deleted_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_delete_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    reply_id BIGINT,
    merchant_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',
    reporter_user_id VARCHAR(20),
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_id VARCHAR(20),
    admin_remarks VARCHAR(255),
    created_at DATETIME NOT NULL,
    processed_at DATETIME,
    INDEX idx_review_delete_req_status (status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 支付
-- ============================================================

CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    pay_method VARCHAR(20) NOT NULL DEFAULT 'MOCK',
    status VARCHAR(20) NOT NULL,
    create_time DATETIME NOT NULL,
    idempotency_key VARCHAR(128) NULL,
    INDEX idx_payment_order (order_id),
    UNIQUE KEY uk_payment_user_idempotency (user_id, idempotency_key),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 公告（含置顶和有效期）
-- ============================================================

CREATE TABLE announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    start_at DATETIME,
    end_at DATETIME,
    create_time DATETIME NOT NULL,
    INDEX idx_announcement_published (status, pinned DESC, create_time DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 服务预约
-- ============================================================

CREATE TABLE service_booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    appointment_time DATETIME NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_booking_user (user_id),
    INDEX idx_booking_merchant (merchant_id, status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 团购
-- ============================================================

CREATE TABLE group_deal (
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
    updated_at DATETIME NOT NULL,
    INDEX idx_group_deal_merchant (merchant_id, status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE deal_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    voucher_code VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    pay_amount INT NOT NULL,
    paid_time DATETIME,
    expire_time DATETIME,
    create_time DATETIME NOT NULL,
    used_time DATETIME,
    INDEX idx_deal_order_user (user_id, status),
    INDEX idx_deal_order_merchant (merchant_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE deal_redeem_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_order_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    voucher_code VARCHAR(40) NOT NULL,
    operator_id VARCHAR(20) NOT NULL,
    redeemed_at DATETIME NOT NULL,
    INDEX idx_redeem_log_order (deal_order_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 优惠券
-- ============================================================

CREATE TABLE coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    coupon_type VARCHAR(20) NOT NULL DEFAULT 'FIXED',
    discount_amount INT NOT NULL DEFAULT 0,
    discount_percent INT,
    min_order_amount INT NOT NULL DEFAULT 0,
    merchant_id BIGINT,
    total_limit INT NOT NULL DEFAULT 0,
    claimed_count INT NOT NULL DEFAULT 0,
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    INDEX idx_coupon_status (status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    order_id BIGINT,
    claimed_at DATETIME NOT NULL,
    used_at DATETIME,
    UNIQUE KEY uk_user_coupon (user_id, coupon_id),
    INDEX idx_user_coupon_user_status (user_id, status),
    INDEX idx_user_coupon_order_status (order_id, status),
    CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon(id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 违规与申诉
-- ============================================================

CREATE TABLE user_penalty (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    penalty_type VARCHAR(30) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    admin_id VARCHAR(20) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    INDEX idx_penalty_user (user_id, active)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE appeal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    penalty_id BIGINT,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_reply TEXT,
    admin_id VARCHAR(20),
    created_at DATETIME NOT NULL,
    processed_at DATETIME,
    INDEX idx_appeal_user (user_id),
    INDEX idx_appeal_status (status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 种子数据（BCrypt 密码哈希 → 明文: Abc123!）
-- ============================================================

INSERT INTO `user` (phone, username, password, role, enabled) VALUES
    ('13800000001', 'user', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000002', 'merchant', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'MERCHANT', 1),
    ('13800000003', 'admin', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'ADMIN', 1),
    ('13800000004', 'rider', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'RIDER', 1),
    ('13800000012', 'merchant2', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'MERCHANT', 1);

INSERT INTO merchant (id, user_id, merchant_name, phone, category, address, longitude, latitude, delivery_radius_m, business_hours, delivery_fee, min_order_price, average_price, score, status, created_at, updated_at) VALUES
    (1, '13800000002', '校园轻食铺', '13800000022', '美食', '软件园东门 1 号', 116.397428, 39.909230, 10000, '09:00-21:00', 300, 1500, 2800, 4.70, 'OPEN', NOW(), NOW()),
    (2, '13800000012', '城市咖啡站', '13800000023', '饮品', '创新街 18 号', 116.405285, 39.904989, 10000, '08:30-22:30', 200, 1200, 2200, 4.50, 'OPEN', NOW(), NOW());

INSERT INTO product_category (id, merchant_id, name, sort_order, created_at, updated_at) VALUES
    (1, 1, '主食', 10, NOW(), NOW()),
    (2, 1, '饮品', 20, NOW(), NOW()),
    (3, 2, '咖啡', 10, NOW(), NOW());

INSERT INTO product (id, merchant_id, category_id, name, description, price, stock, image, status, created_at, updated_at) VALUES
    (1, 1, 1, '鸡胸肉能量碗', '健康低卡能量满满', 2590, 30, '/images/product-1.jpg', 'ON_SALE', NOW(), NOW()),
    (2, 1, 1, '牛油果沙拉', '新鲜牛油果配时蔬', 2290, 24, '/images/product-2.jpg', 'ON_SALE', NOW(), NOW()),
    (3, 1, 2, '低糖酸奶杯', '低糖酸奶搭配燕麦', 1290, 40, '/images/product-3.jpg', 'ON_SALE', NOW(), NOW()),
    (4, 2, 3, '拿铁', '经典意式拿铁', 1800, 50, '/images/product-4.jpg', 'ON_SALE', NOW(), NOW()),
    (5, 2, 3, '冷萃咖啡', '低温慢萃咖啡', 2200, 35, '/images/product-5.jpg', 'ON_SALE', NOW(), NOW());

INSERT INTO user_address (id, user_id, contact_name, phone, address, longitude, latitude, is_default, created_at, updated_at) VALUES
    (1, '13800000001', '张同学', '13800000001', '软件学院 A 座 302', 116.398000, 39.910000, 1, NOW(), NOW());

INSERT INTO announcement (id, title, content, status, pinned, start_at, end_at, create_time) VALUES
    (1, '平台公告', '欢迎使用 CLAS 生活助手平台演示版。', 'PUBLISHED', 0, NOW(), NULL, NOW());

INSERT INTO service_booking (id, user_id, merchant_id, service_name, appointment_time, contact_phone, note, status, created_at, updated_at) VALUES
    (1, '13800000001', 1, '门店轻食咨询', DATE_ADD(NOW(), INTERVAL 1 DAY), '13800000001', '希望安排下午到店', 'CONFIRMED', NOW(), NOW());

INSERT INTO group_deal (id, merchant_id, title, description, original_price, deal_price, stock, valid_days, status, created_at, updated_at) VALUES
    (1, 1, '双人轻食套餐', '任选两份主食加酸奶杯，到店核销更划算', 6400, 4990, 50, 30, 'ON_SALE', NOW(), NOW()),
    (2, 2, '咖啡下午茶券', '拿铁或冷萃任选一杯，工作日下午可用', 2200, 1590, 80, 15, 'ON_SALE', NOW(), NOW());

INSERT INTO coupon (id, title, description, coupon_type, discount_amount, min_order_amount, merchant_id, total_limit, claimed_count, valid_from, valid_to, status, created_at) VALUES
    (1, '新用户满减券', '外卖订单满 ¥20 减 ¥3', 'FIXED', 300, 2000, NULL, 1000, 0, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 'ACTIVE', NOW()),
    (2, '轻食铺专享券', '校园轻食铺满 ¥15 减 ¥2', 'FIXED', 200, 1500, 1, 500, 0, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 'ACTIVE', NOW());
