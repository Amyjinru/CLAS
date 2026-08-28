-- Reduce login and authenticated-request penalty lookup cost.

USE clas;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_login_index_if_missing $$
CREATE PROCEDURE add_login_index_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'user_penalty'
          AND index_name = 'idx_penalty_user_type_active_end'
    ) THEN
        ALTER TABLE user_penalty
            ADD INDEX idx_penalty_user_type_active_end (user_id, penalty_type, active, end_time);
    END IF;
END $$

DELIMITER ;

CALL add_login_index_if_missing();
DROP PROCEDURE IF EXISTS add_login_index_if_missing;
