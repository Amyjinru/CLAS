-- 用户举报评论：删评申请支持来源类型与回复目标

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

CALL add_column_if_missing('review_delete_request', 'request_type', 'request_type VARCHAR(20) NOT NULL DEFAULT ''MERCHANT'' AFTER merchant_id');
CALL add_column_if_missing('review_delete_request', 'reporter_user_id', 'reporter_user_id VARCHAR(20) NULL AFTER request_type');
CALL add_column_if_missing('review_delete_request', 'reply_id', 'reply_id BIGINT NULL AFTER review_id');

UPDATE review_delete_request SET request_type = 'MERCHANT' WHERE request_type IS NULL OR request_type = '';

DROP PROCEDURE IF EXISTS add_column_if_missing;
