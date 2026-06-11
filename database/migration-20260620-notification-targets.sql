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

CALL add_column_if_missing('notification', 'type', 'type VARCHAR(50) AFTER read_flag');
CALL add_column_if_missing('notification', 'target_type', 'target_type VARCHAR(50) AFTER type');
CALL add_column_if_missing('notification', 'target_id', 'target_id BIGINT AFTER target_type');
CALL add_column_if_missing('notification', 'review_id', 'review_id BIGINT AFTER target_id');
CALL add_column_if_missing('notification', 'reply_id', 'reply_id BIGINT AFTER review_id');
CALL add_column_if_missing('notification', 'order_id', 'order_id BIGINT AFTER reply_id');
CALL add_column_if_missing('notification', 'merchant_id', 'merchant_id BIGINT AFTER order_id');
CALL add_column_if_missing('notification', 'target_path', 'target_path VARCHAR(255) AFTER merchant_id');

DROP PROCEDURE IF EXISTS add_column_if_missing;
