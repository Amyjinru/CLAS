CREATE DATABASE IF NOT EXISTS clas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clas;

DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS merchant_audit_log;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) UNIQUE,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    merchant_name VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    category VARCHAR(50),
    address VARCHAR(255),
    score DECIMAL(3,2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    bank_account VARCHAR(50),
    admin_remarks VARCHAR(255),
    settlement_cycle INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE merchant_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
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
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    total_price INT NOT NULL,
    status VARCHAR(20) NOT NULL,
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
    user_id BIGINT NOT NULL,
    score INT NOT NULL,
    content TEXT
);

INSERT INTO `user` (id, username, password, phone, role) VALUES
    (1, 'user', '123456', '13800000001', 'USER'),
    (2, 'merchant', '123456', '13800000002', 'MERCHANT'),
    (3, 'admin', '123456', '13800000003', 'ADMIN');

INSERT INTO merchant (id, user_id, merchant_name, phone, category, address, score, status, created_at, updated_at) VALUES
    (1, 2, '校园轻食铺', '13800000002', '美食', '软件园东门 1 号', 4.70, 'OPEN', NOW(), NOW()),
    (2, 2, '城市咖啡站', '13800000004', '饮品', '创新街 18 号', 4.50, 'OPEN', NOW(), NOW());

INSERT INTO product (id, merchant_id, name, description, price, stock, image, status, created_at, updated_at) VALUES
    (1, 1, '鸡胸肉能量碗', '健康低卡能量满满', 2590, 30, '/images/product-1.jpg', 'ON_SALE', NOW(), NOW()),
    (2, 1, '牛油果沙拉', '富含优质脂肪', 2290, 24, '/images/product-2.jpg', 'ON_SALE', NOW(), NOW()),
    (3, 1, '低糖酸奶杯', '无糖配方更健康', 1290, 40, '/images/product-3.jpg', 'ON_SALE', NOW(), NOW()),
    (4, 2, '拿铁', '精选浓缩咖啡与鲜牛奶', 1800, 50, '/images/product-4.jpg', 'ON_SALE', NOW(), NOW()),
    (5, 2, '冷萃咖啡', '低温慢速萃取', 2200, 35, '/images/product-5.jpg', 'ON_SALE', NOW(), NOW());
