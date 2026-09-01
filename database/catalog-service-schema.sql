-- Apply with a database administrator account before deploying catalog-service.
-- The catalog service account receives privileges only on this schema.
CREATE DATABASE IF NOT EXISTS catalog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Create the catalog_service account through the deployment secret/DB administration process,
-- then grant it only SELECT, INSERT, UPDATE and DELETE on catalog_db.*.

USE catalog_db;

CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_category_merchant_name (merchant_id, name),
    KEY idx_product_category_merchant (merchant_id, sort_order, id)
);

CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    price INT NOT NULL,
    stock INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    image VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_product_merchant_status (merchant_id, status, id),
    KEY idx_product_category_id (category_id)
);

-- Migrate product and product_category data once from the monolith schema before routing traffic.
-- No other service account is granted privileges on catalog_db.
