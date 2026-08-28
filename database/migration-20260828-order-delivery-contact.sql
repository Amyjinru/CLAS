-- 为订单保存支付前确认的配送联系人快照。

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

CALL add_column_if_missing('orders', 'delivery_contact_name', 'delivery_contact_name VARCHAR(50) NULL AFTER delivery_address');
CALL add_column_if_missing('orders', 'delivery_contact_phone', 'delivery_contact_phone VARCHAR(20) NULL AFTER delivery_contact_name');

DROP PROCEDURE IF EXISTS add_column_if_missing;
