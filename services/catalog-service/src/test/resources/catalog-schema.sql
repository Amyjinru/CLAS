CREATE TABLE product_category (
    id BIGINT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE product (
    id BIGINT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price INT NOT NULL,
    stock INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    image VARCHAR(500)
);

INSERT INTO product_category (id, merchant_id, name, sort_order) VALUES (10, 1, '主食', 1);
INSERT INTO product (id, merchant_id, category_id, name, description, price, stock, status, image)
VALUES (100, 1, 10, '牛肉饭', '测试商品', 2800, 5, 'ON_SALE', '/beef.jpg');
INSERT INTO product (id, merchant_id, category_id, name, description, price, stock, status, image)
VALUES (101, 1, 10, '下架商品', '不应公开', 1800, 9, 'OFF_SALE', '/off.jpg');
