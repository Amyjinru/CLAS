CREATE DATABASE IF NOT EXISTS clas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clas;

DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS service_booking;
DROP TABLE IF EXISTS deal_order;
DROP TABLE IF EXISTS group_deal;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS merchant_audit_log;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    phone VARCHAR(20) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    category VARCHAR(50),
    address VARCHAR(255),
    business_hours VARCHAR(100),
    delivery_fee INT NOT NULL DEFAULT 0,
    min_order_price INT NOT NULL DEFAULT 0,
    average_price INT NOT NULL DEFAULT 0,
    score DECIMAL(3,2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    bank_account VARCHAR(50),
    admin_remarks VARCHAR(255),
    settlement_cycle INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_merchant_user_id (user_id)
);

CREATE TABLE merchant_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    admin_id VARCHAR(20) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    created_at DATETIME NOT NULL
);

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL
);

CREATE TABLE user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_favorite_user_merchant (user_id, merchant_id)
);

CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    read_flag TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    total_price INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(255),
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    estimated_minutes INT NOT NULL DEFAULT 30,
    refund_reason VARCHAR(255),
    refund_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    create_time DATETIME NOT NULL
);

CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price INT NOT NULL
);

CREATE TABLE review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    content TEXT,
    merchant_reply TEXT,
    report_reason VARCHAR(255),
    report_status VARCHAR(20) NOT NULL DEFAULT 'NONE'
);

CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    pay_method VARCHAR(20) NOT NULL DEFAULT 'MOCK',
    status VARCHAR(20) NOT NULL,
    create_time DATETIME NOT NULL
);

CREATE TABLE announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    create_time DATETIME NOT NULL
);

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
    updated_at DATETIME NOT NULL
);

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
    updated_at DATETIME NOT NULL
);

CREATE TABLE deal_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    voucher_code VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    pay_amount INT NOT NULL,
    create_time DATETIME NOT NULL,
    used_time DATETIME
);

INSERT INTO `user` (phone, username, password, role) VALUES
    ('13800000001', 'user', 'Abc123!', 'USER'),
    ('13800000002', 'merchant', 'Abc123!', 'MERCHANT'),
    ('13800000003', 'admin', 'Abc123!', 'ADMIN'),
    ('13800000012', 'merchant2', 'Abc123!', 'MERCHANT');

INSERT INTO merchant (id, user_id, merchant_name, phone, category, address, business_hours, delivery_fee, min_order_price, average_price, score, status, created_at, updated_at) VALUES
    (1, '13800000002', '校园轻食铺', '13800000022', '美食', '软件园东门 1 号', '09:00-21:00', 300, 1500, 2800, 4.70, 'OPEN', NOW(), NOW()),
    (2, '13800000012', '城市咖啡站', '13800000023', '饮品', '创新街 18 号', '08:30-22:30', 200, 1200, 2200, 4.50, 'OPEN', NOW(), NOW());

INSERT INTO product (id, merchant_id, name, description, price, stock, image, status, created_at, updated_at) VALUES
    (1, 1, '鸡胸肉能量碗', '健康低卡能量满满', 2590, 30, '/images/product-1.jpg', 'ON_SALE', NOW(), NOW()),
    (2, 1, '牛油果沙拉', '新鲜牛油果配时蔬', 2290, 24, '/images/product-2.jpg', 'ON_SALE', NOW(), NOW()),
    (3, 1, '低糖酸奶杯', '低糖酸奶搭配燕麦', 1290, 40, '/images/product-3.jpg', 'ON_SALE', NOW(), NOW()),
    (4, 2, '拿铁', '经典意式拿铁', 1800, 50, '/images/product-4.jpg', 'ON_SALE', NOW(), NOW()),
    (5, 2, '冷萃咖啡', '低温慢萃咖啡', 2200, 35, '/images/product-5.jpg', 'ON_SALE', NOW(), NOW());

INSERT INTO user_address (id, user_id, contact_name, phone, address, is_default, created_at, updated_at) VALUES
    (1, '13800000001', '张同学', '13800000001', '软件学院 A 座 302', 1, NOW(), NOW());

INSERT INTO service_booking (id, user_id, merchant_id, service_name, appointment_time, contact_phone, note, status, created_at, updated_at) VALUES
    (1, '13800000001', 1, '门店轻食咨询', DATE_ADD(NOW(), INTERVAL 1 DAY), '13800000001', '希望安排下午到店', 'CONFIRMED', NOW(), NOW());

INSERT INTO group_deal (id, merchant_id, title, description, original_price, deal_price, stock, valid_days, status, created_at, updated_at) VALUES
    (1, 1, '双人轻食套餐', '任选两份主食加酸奶杯，到店核销更划算', 6400, 4990, 50, 30, 'ON_SALE', NOW(), NOW()),
    (2, 2, '咖啡下午茶券', '拿铁或冷萃任选一杯，工作日下午可用', 2200, 1590, 80, 15, 'ON_SALE', NOW(), NOW());
