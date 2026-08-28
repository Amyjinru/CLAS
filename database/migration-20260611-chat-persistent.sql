USE clas;

DELIMITER $$

CREATE PROCEDURE alter_chat_order_nullable_if_exists()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'chat_message'
    ) THEN
        ALTER TABLE chat_message MODIFY COLUMN order_id BIGINT NULL;
    END IF;
END$$

DELIMITER ;

CALL alter_chat_order_nullable_if_exists();
DROP PROCEDURE alter_chat_order_nullable_if_exists;
