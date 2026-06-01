CREATE DATABASE IF NOT EXISTS clas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clas;

DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL
);

CREATE TABLE merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    address VARCHAR(255),
    score DECIMAL(3,2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE'
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

INSERT INTO merchant (id, merchant_name, category, address, score, status) VALUES
    (1, '校园轻食铺', '美食', '软件园东门 1 号', 4.70, 'OPEN'),
    (2, '城市咖啡站', '饮品', '创新街 18 号', 4.50, 'OPEN');

INSERT INTO product (id, merchant_id, name, price, stock, image, status) VALUES
    (1, 1, '鸡胸肉能量碗', 2590, 30, '/images/product-1.jpg', 'ON_SALE'),
    (2, 1, '牛油果沙拉', 2290, 24, '/images/product-2.jpg', 'ON_SALE'),
    (3, 1, '低糖酸奶杯', 1290, 40, '/images/product-3.jpg', 'ON_SALE'),
    (4, 2, '拿铁', 1800, 50, '/images/product-4.jpg', 'ON_SALE'),
    (5, 2, '冷萃咖啡', 2200, 35, '/images/product-5.jpg', 'ON_SALE');
