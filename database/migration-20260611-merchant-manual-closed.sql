USE clas;

-- 幂等：仅在列不存在时才添加
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'clas' AND TABLE_NAME = 'merchant'
                   AND COLUMN_NAME = 'manual_closed');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE merchant ADD COLUMN manual_closed TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT "Column manual_closed already exists, skipping."');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
