CREATE TABLE IF NOT EXISTS "user" (
    phone VARCHAR(20) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
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
    bank_account VARCHAR(50),
    admin_remarks VARCHAR(255),
    settlement_cycle INT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 测试环境也保留审核日志表，保证管理员权限相关接口可以被集成测试覆盖。
CREATE TABLE IF NOT EXISTS merchant_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    admin_id VARCHAR(20) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, merchant_id)
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    total_price INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(255),
    delivery_longitude DECIMAL(10,6),
    delivery_latitude DECIMAL(10,6),
    distance_meters INT,
    route_distance_meters INT,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    estimated_minutes INT NOT NULL DEFAULT 30,
    refund_reason VARCHAR(255),
    refund_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    create_time TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price INT NOT NULL
);

CREATE TABLE IF NOT EXISTS review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    content CLOB,
    merchant_reply CLOB,
    report_reason VARCHAR(255),
    report_status VARCHAR(20) NOT NULL DEFAULT 'NONE'
);

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    pay_method VARCHAR(20) NOT NULL DEFAULT 'MOCK',
    status VARCHAR(20) NOT NULL,
    create_time TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content CLOB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    create_time TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS service_booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    appointment_time TIMESTAMP NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

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
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS deal_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    voucher_code VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    pay_amount INT NOT NULL,
    create_time TIMESTAMP NOT NULL,
    used_time TIMESTAMP
);

DELETE FROM announcement;
DELETE FROM service_booking;
DELETE FROM deal_order;
DELETE FROM group_deal;
DELETE FROM notification;
DELETE FROM favorite;
DELETE FROM user_address;
DELETE FROM payment;
DELETE FROM review;
DELETE FROM order_item;
DELETE FROM orders;
DELETE FROM cart;
DELETE FROM product;
DELETE FROM merchant_audit_log;
DELETE FROM merchant;
DELETE FROM "user";

-- 演示账号在测试库中也保持 USER / MERCHANT / ADMIN 三角色齐全。
INSERT INTO "user" (phone, username, password, role) VALUES
    ('13800000001', 'user', 'Abc123!', 'USER'),
    ('13800000002', 'merchant', 'Abc123!', 'MERCHANT'),
    ('13800000003', 'admin', 'Abc123!', 'ADMIN');

INSERT INTO merchant (id, user_id, merchant_name, phone, category, address, longitude, latitude, delivery_radius_m, business_hours, delivery_fee, min_order_price, average_price, score, status, created_at, updated_at) VALUES
    (1, '13800000002', '校园轻食铺', '13800000022', '美食', '软件园东门 1 号', 116.397428, 39.909230, 3000, '09:00-21:00', 300, 1500, 2800, 4.70, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product (id, merchant_id, name, description, price, stock, image, status, created_at, updated_at) VALUES
    (1, 1, '鸡胸肉能量碗', '健康低卡能量满满', 2590, 30, '/images/product-1.jpg', 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO announcement (id, title, content, status, create_time) VALUES
    (1, '测试公告', 'H2 集成测试公告', 'PUBLISHED', CURRENT_TIMESTAMP);

INSERT INTO user_address (id, user_id, contact_name, phone, address, longitude, latitude, is_default, created_at, updated_at) VALUES
    (1, '13800000001', '张同学', '13800000001', '软件学院 A 座 302', 116.398000, 39.910000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO service_booking (id, user_id, merchant_id, service_name, appointment_time, contact_phone, note, status, created_at, updated_at) VALUES
    (1, '13800000001', 1, '门店轻食咨询', DATEADD('DAY', 1, CURRENT_TIMESTAMP), '13800000001', '希望安排下午到店', 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO group_deal (id, merchant_id, title, description, original_price, deal_price, stock, valid_days, status, created_at, updated_at) VALUES
    (1, 1, '双人轻食套餐', '任选两份主食加酸奶杯，到店核销更划算', 6400, 4990, 50, 30, 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE merchant ALTER COLUMN id RESTART WITH 10;
ALTER TABLE merchant_audit_log ALTER COLUMN id RESTART WITH 10;
ALTER TABLE product ALTER COLUMN id RESTART WITH 10;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 100;
ALTER TABLE announcement ALTER COLUMN id RESTART WITH 10;
ALTER TABLE user_address ALTER COLUMN id RESTART WITH 10;
ALTER TABLE favorite ALTER COLUMN id RESTART WITH 10;
ALTER TABLE notification ALTER COLUMN id RESTART WITH 10;
ALTER TABLE service_booking ALTER COLUMN id RESTART WITH 10;
ALTER TABLE group_deal ALTER COLUMN id RESTART WITH 10;
ALTER TABLE deal_order ALTER COLUMN id RESTART WITH 10;
