-- 登录设备识别与异机确认：同一浏览器重登不触发验证码，异机请求可提示当前在线用户。
USE clas;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_login_risk_column_if_missing(
    IN p_column VARCHAR(64), IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `user` ADD COLUMN ', p_definition);
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_login_risk_column_if_missing('session_device_id', 'session_device_id VARCHAR(100) NULL AFTER session_expires_at');
CALL add_login_risk_column_if_missing('session_last_seen_at', 'session_last_seen_at DATETIME NULL AFTER session_device_id');
CALL add_login_risk_column_if_missing('pending_login_challenge_id', 'pending_login_challenge_id VARCHAR(64) NULL AFTER session_last_seen_at');
CALL add_login_risk_column_if_missing('pending_login_device_id', 'pending_login_device_id VARCHAR(100) NULL AFTER pending_login_challenge_id');
CALL add_login_risk_column_if_missing('pending_login_created_at', 'pending_login_created_at DATETIME NULL AFTER pending_login_device_id');

DROP PROCEDURE IF EXISTS add_login_risk_column_if_missing;
