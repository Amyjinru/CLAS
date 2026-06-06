CREATE TABLE IF NOT EXISTS "user" (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) UNIQUE,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    category VARCHAR(50),
    address VARCHAR(255),
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
    admin_id BIGINT NOT NULL,
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
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    total_price INT NOT NULL,
    status VARCHAR(20) NOT NULL,
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
    user_id BIGINT NOT NULL,
    score INT NOT NULL,
    content CLOB
);

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
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

DELETE FROM announcement;
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
INSERT INTO "user" (id, username, password, phone, role) VALUES
    (1, 'user', '123456', '13800000001', 'USER'),
    (2, 'merchant', '123456', '13800000002', 'MERCHANT'),
    (3, 'admin', '123456', '13800000003', 'ADMIN');

INSERT INTO merchant (id, user_id, merchant_name, phone, category, address, score, status, created_at, updated_at) VALUES
    (1, 2, '校园轻食铺', '13800000002', '美食', '软件园东门 1 号', 4.70, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product (id, merchant_id, name, description, price, stock, image, status, created_at, updated_at) VALUES
    (1, 1, '鸡胸肉能量碗', '健康低卡能量满满', 2590, 30, '/images/product-1.jpg', 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO announcement (id, title, content, status, create_time) VALUES
    (1, '测试公告', 'H2 集成测试公告', 'PUBLISHED', CURRENT_TIMESTAMP);

ALTER TABLE "user" ALTER COLUMN id RESTART WITH 10;
ALTER TABLE merchant ALTER COLUMN id RESTART WITH 10;
ALTER TABLE merchant_audit_log ALTER COLUMN id RESTART WITH 10;
ALTER TABLE product ALTER COLUMN id RESTART WITH 10;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 100;
ALTER TABLE announcement ALTER COLUMN id RESTART WITH 10;
