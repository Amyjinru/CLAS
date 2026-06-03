CREATE TABLE IF NOT EXISTS "user" (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    address VARCHAR(255),
    score DECIMAL(3,2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE'
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
DELETE FROM merchant;
DELETE FROM "user";

INSERT INTO "user" (id, username, password, phone, role) VALUES
    (1, 'user', '123456', '13800000001', 'USER');

INSERT INTO merchant (id, merchant_name, category, address, score, status) VALUES
    (1, '校园轻食铺', '美食', '软件园东门 1 号', 4.70, 'OPEN');

INSERT INTO product (id, merchant_id, name, price, stock, image, status) VALUES
    (1, 1, '鸡胸肉能量碗', 2590, 30, '/images/product-1.jpg', 'ON_SALE');

INSERT INTO announcement (id, title, content, status, create_time) VALUES
    (1, '测试公告', 'H2 集成测试公告', 'PUBLISHED', CURRENT_TIMESTAMP);

ALTER TABLE "user" ALTER COLUMN id RESTART WITH 10;
ALTER TABLE merchant ALTER COLUMN id RESTART WITH 10;
ALTER TABLE product ALTER COLUMN id RESTART WITH 10;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 100;
ALTER TABLE announcement ALTER COLUMN id RESTART WITH 10;
