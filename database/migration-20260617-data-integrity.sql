-- Core relationship constraints.
-- Review database/orphan-detection-core.sql before running this migration.
-- The cleanup statements below remove rows that cannot be interpreted without their parent.

USE clas;

DELETE oi FROM order_item oi LEFT JOIN orders o ON o.id = oi.order_id WHERE o.id IS NULL;
DELETE oi FROM order_item oi LEFT JOIN product p ON p.id = oi.product_id WHERE p.id IS NULL;
DELETE p FROM payment p LEFT JOIN orders o ON o.id = p.order_id WHERE o.id IS NULL;
DELETE r FROM review r LEFT JOIN orders o ON o.id = r.order_id WHERE o.id IS NULL;
DELETE uc FROM user_coupon uc LEFT JOIN coupon c ON c.id = uc.coupon_id WHERE c.id IS NULL;

-- 幂等：仅在约束不存在时才添加
DELIMITER $$

DROP PROCEDURE IF EXISTS add_constraint_if_missing $$
CREATE PROCEDURE add_constraint_if_missing(
    IN table_name_value VARCHAR(64),
    IN constraint_name_value VARCHAR(64),
    IN constraint_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        WHERE tc.constraint_schema = DATABASE()
          AND tc.table_name = table_name_value
          AND tc.constraint_name = constraint_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD CONSTRAINT `', constraint_name_value, '` ', constraint_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_constraint_if_missing('order_item', 'fk_order_item_order', 'FOREIGN KEY (order_id) REFERENCES orders(id)');
CALL add_constraint_if_missing('order_item', 'fk_order_item_product', 'FOREIGN KEY (product_id) REFERENCES product(id)');
CALL add_constraint_if_missing('payment', 'fk_payment_order', 'FOREIGN KEY (order_id) REFERENCES orders(id)');
CALL add_constraint_if_missing('review', 'fk_review_order', 'FOREIGN KEY (order_id) REFERENCES orders(id)');
CALL add_constraint_if_missing('user_coupon', 'fk_user_coupon_coupon', 'FOREIGN KEY (coupon_id) REFERENCES coupon(id)');

DROP PROCEDURE IF EXISTS add_constraint_if_missing;
