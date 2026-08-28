-- 单设备会话：新登录会写入唯一会话标识，使之前签发的 JWT 立即失效。
USE clas;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_column_if_missing(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_definition);
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_column_if_missing('user', 'session_token', 'session_token VARCHAR(64) NULL AFTER nickname');
CALL add_column_if_missing('user', 'session_expires_at', 'session_expires_at DATETIME NULL AFTER session_token');

DROP PROCEDURE IF EXISTS add_column_if_missing;
