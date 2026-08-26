-- Rider information module: repeatable, additive MySQL migration.
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

CALL add_column_if_missing('rider_profile', 'service_phone', 'service_phone VARCHAR(20) NULL AFTER emergency_contact_phone');

CREATE TABLE IF NOT EXISTS rider_profile_change_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rider_id VARCHAR(20) NOT NULL,
    current_phone VARCHAR(20) NOT NULL,
    requested_phone VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    review_reason VARCHAR(255),
    reviewer_id VARCHAR(20),
    reviewed_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_rider_profile_change_pending (status, created_at),
    INDEX idx_rider_profile_change_rider (rider_id, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE rider_profile SET service_phone = user_id WHERE service_phone IS NULL OR service_phone = '';

DROP PROCEDURE IF EXISTS add_column_if_missing;
