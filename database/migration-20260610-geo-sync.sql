-- 同步 dev 版地图/配送相关字段（解决切换页面系统异常）
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

CALL add_column_if_missing('merchant', 'longitude', 'longitude DECIMAL(10,6)');
CALL add_column_if_missing('merchant', 'latitude', 'latitude DECIMAL(10,6)');
CALL add_column_if_missing('merchant', 'delivery_radius_m', 'delivery_radius_m INT NOT NULL DEFAULT 3000');

CALL add_column_if_missing('user_address', 'longitude', 'longitude DECIMAL(10,6)');
CALL add_column_if_missing('user_address', 'latitude', 'latitude DECIMAL(10,6)');

CALL add_column_if_missing('orders', 'delivery_longitude', 'delivery_longitude DECIMAL(10,6)');
CALL add_column_if_missing('orders', 'delivery_latitude', 'delivery_latitude DECIMAL(10,6)');
CALL add_column_if_missing('orders', 'distance_meters', 'distance_meters INT');
CALL add_column_if_missing('orders', 'route_distance_meters', 'route_distance_meters INT');

UPDATE merchant
SET longitude = COALESCE(longitude, 116.397428),
    latitude = COALESCE(latitude, 39.90923),
    delivery_radius_m = COALESCE(delivery_radius_m, 3000)
WHERE id IN (1, 2);

UPDATE user_address
SET longitude = COALESCE(longitude, 116.397428),
    latitude = COALESCE(latitude, 39.90923)
WHERE longitude IS NULL OR latitude IS NULL;

DROP PROCEDURE IF EXISTS add_column_if_missing;
