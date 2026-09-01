-- 公告发布窗口字段补齐：兼容旧服务器库只存在基础 announcement 表的情况
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

CALL add_column_if_missing('announcement', 'pinned', 'pinned TINYINT(1) NOT NULL DEFAULT 0 AFTER status');
CALL add_column_if_missing('announcement', 'start_at', 'start_at DATETIME NULL AFTER pinned');
CALL add_column_if_missing('announcement', 'end_at', 'end_at DATETIME NULL AFTER start_at');

UPDATE announcement SET pinned = 0 WHERE pinned IS NULL;

DROP PROCEDURE IF EXISTS add_column_if_missing;
