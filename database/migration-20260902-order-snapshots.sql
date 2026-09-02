-- Immutable display snapshots make historical order responses independent of later
-- catalog or merchant edits. Existing records retain NULL and use read-time fallback.
USE clas;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_order_display_snapshots_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders'
          AND COLUMN_NAME = 'merchant_name_snapshot'
    ) THEN
        ALTER TABLE orders ADD COLUMN merchant_name_snapshot VARCHAR(100) NULL AFTER client_request_key;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders'
          AND COLUMN_NAME = 'merchant_logo_snapshot'
    ) THEN
        ALTER TABLE orders ADD COLUMN merchant_logo_snapshot VARCHAR(512) NULL AFTER merchant_name_snapshot;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item'
          AND COLUMN_NAME = 'product_name_snapshot'
    ) THEN
        ALTER TABLE order_item ADD COLUMN product_name_snapshot VARCHAR(100) NULL AFTER price;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item'
          AND COLUMN_NAME = 'product_image_snapshot'
    ) THEN
        ALTER TABLE order_item ADD COLUMN product_image_snapshot VARCHAR(255) NULL AFTER product_name_snapshot;
    END IF;
END //
DELIMITER ;

CALL add_order_display_snapshots_if_missing();
DROP PROCEDURE IF EXISTS add_order_display_snapshots_if_missing;
