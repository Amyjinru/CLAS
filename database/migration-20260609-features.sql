-- CLAS 新功能迁移：推荐、评价增强、违规处理、个人资料
CREATE DATABASE IF NOT EXISTS clas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clas;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing $$
CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns c
        WHERE c.table_schema = DATABASE()
          AND c.table_name = table_name_value
          AND c.column_name = column_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN ', column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

-- 用户资料
CALL add_column_if_missing('user', 'avatar', 'avatar VARCHAR(512)');
CALL add_column_if_missing('user', 'nickname', 'nickname VARCHAR(50)');

-- 评价增强
CALL add_column_if_missing('review', 'created_at', 'created_at DATETIME');
UPDATE review SET created_at = COALESCE(created_at, NOW());

CREATE TABLE IF NOT EXISTS review_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS review_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    parent_reply_id BIGINT,
    user_id VARCHAR(20) NOT NULL,
    reply_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    content TEXT NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS review_vote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    vote_type VARCHAR(10) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_review_vote (target_type, target_id, user_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS review_user_hidden (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_review_hidden (review_id, user_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deleted_review_backup (
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

CREATE TABLE IF NOT EXISTS review_delete_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    reply_id BIGINT NULL,
    merchant_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',
    reporter_user_id VARCHAR(20) NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_id VARCHAR(20),
    admin_remarks VARCHAR(255),
    created_at DATETIME NOT NULL,
    processed_at DATETIME
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 违规处理与申诉
CREATE TABLE IF NOT EXISTS user_penalty (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    penalty_type VARCHAR(20) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    admin_id VARCHAR(20) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS appeal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    penalty_id BIGINT,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_reply TEXT,
    admin_id VARCHAR(20),
    created_at DATETIME NOT NULL,
    processed_at DATETIME
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_column_if_missing;
