-- Query performance indexes for dashboard statistics, ranking queries, and batched review/order details.

USE clas;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_index_if_missing $$
CREATE PROCEDURE add_index_if_missing(
    IN table_name_value VARCHAR(64),
    IN index_name_value VARCHAR(64),
    IN index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics s
        WHERE s.table_schema = DATABASE()
          AND s.table_name = table_name_value
          AND s.index_name = index_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD INDEX `', index_name_value, '` ', index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_index_if_missing('orders', 'idx_orders_create_status', '(create_time, status)');
CALL add_index_if_missing('orders', 'idx_orders_merchant_create_status', '(merchant_id, create_time, status)');
CALL add_index_if_missing('order_item', 'idx_order_item_product_order', '(product_id, order_id)');
CALL add_index_if_missing('review', 'idx_review_order_id', '(order_id, id)');
CALL add_index_if_missing('review_image', 'idx_review_image_review_sort', '(review_id, sort_order)');
CALL add_index_if_missing('review_reply', 'idx_review_reply_review_deleted_id', '(review_id, deleted, id)');
CALL add_index_if_missing('review_vote', 'idx_review_vote_target', '(target_type, target_id)');
CALL add_index_if_missing('review_user_hidden', 'idx_review_hidden_user', '(user_id, review_id)');

DROP PROCEDURE IF EXISTS add_index_if_missing;
