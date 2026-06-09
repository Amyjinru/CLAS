-- D 组：交易闭环字段（订单备注、拒单理由、金额明细快照、支付幂等）

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

CALL add_column_if_missing('orders', 'subtotal', 'subtotal INT NULL AFTER total_price');
CALL add_column_if_missing('orders', 'delivery_fee', 'delivery_fee INT NOT NULL DEFAULT 0 AFTER subtotal');
CALL add_column_if_missing('orders', 'remark', 'remark VARCHAR(255) NULL AFTER refund_status');
CALL add_column_if_missing('orders', 'reject_reason', 'reject_reason VARCHAR(255) NULL AFTER remark');

UPDATE orders SET subtotal = total_price WHERE subtotal IS NULL;

DROP PROCEDURE IF EXISTS add_column_if_missing;
