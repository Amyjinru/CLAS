-- 订单状态时间线：记录关键履约节点时间。

USE clas;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_column_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_column_if_missing('orders', 'paid_at', 'paid_at DATETIME NULL AFTER create_time');
CALL add_column_if_missing('orders', 'accepted_at', 'accepted_at DATETIME NULL AFTER paid_at');
CALL add_column_if_missing('orders', 'delivered_at', 'delivered_at DATETIME NULL AFTER accepted_at');
CALL add_column_if_missing('orders', 'completed_at', 'completed_at DATETIME NULL AFTER delivered_at');
CALL add_column_if_missing('orders', 'canceled_at', 'canceled_at DATETIME NULL AFTER completed_at');
CALL add_column_if_missing('orders', 'rejected_at', 'rejected_at DATETIME NULL AFTER canceled_at');

DROP PROCEDURE IF EXISTS add_column_if_missing;
