-- 把各服务库中的业务表搬回 clas。幂等。不删账号、不删数据。

USE clas;

DROP PROCEDURE IF EXISTS clas_p3_move_table_back;

DELIMITER $$

CREATE PROCEDURE clas_p3_move_table_back(IN src VARCHAR(64), IN tbl VARCHAR(64))
BEGIN
  DECLARE src_cnt INT DEFAULT 0;
  DECLARE dst_cnt INT DEFAULT 0;
  SELECT COUNT(*) INTO src_cnt FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = src AND TABLE_NAME = tbl AND TABLE_TYPE = 'BASE TABLE';
  SELECT COUNT(*) INTO dst_cnt FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'clas' AND TABLE_NAME = tbl AND TABLE_TYPE = 'BASE TABLE';
  IF src_cnt = 1 AND dst_cnt = 0 THEN
    SET @ddl = CONCAT('RENAME TABLE `', src, '`.`', tbl, '` TO clas.`', tbl, '`');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  ELSEIF src_cnt = 1 AND dst_cnt = 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'P3 move-back conflict: table exists in both schemas';
  END IF;
END$$

DELIMITER ;

CALL clas_p3_move_table_back('clas_iam', 'user');
CALL clas_p3_move_table_back('clas_iam', 'user_role');
CALL clas_p3_move_table_back('clas_iam', 'user_address');
CALL clas_p3_move_table_back('clas_iam', 'user_bank_card');
CALL clas_p3_move_table_back('clas_iam', 'favorite');
CALL clas_p3_move_table_back('clas_iam', 'role_application');
CALL clas_p3_move_table_back('clas_iam', 'notification');
CALL clas_p3_move_table_back('clas_iam', 'user_penalty');
CALL clas_p3_move_table_back('clas_iam', 'appeal');

CALL clas_p3_move_table_back('clas_merchant', 'merchant');
CALL clas_p3_move_table_back('clas_merchant', 'merchant_audit_log');

CALL clas_p3_move_table_back('clas_catalog', 'product_category');
CALL clas_p3_move_table_back('clas_catalog', 'product');
CALL clas_p3_move_table_back('clas_catalog', 'group_deal');
CALL clas_p3_move_table_back('clas_catalog', 'service_booking');

CALL clas_p3_move_table_back('clas_order', 'cart');
CALL clas_p3_move_table_back('clas_order', 'orders');
CALL clas_p3_move_table_back('clas_order', 'order_item');
CALL clas_p3_move_table_back('clas_order', 'order_lifecycle_event');
CALL clas_p3_move_table_back('clas_order', 'order_refund_dispute');
CALL clas_p3_move_table_back('clas_order', 'payment');
CALL clas_p3_move_table_back('clas_order', 'deal_order');
CALL clas_p3_move_table_back('clas_order', 'deal_redeem_log');
CALL clas_p3_move_table_back('clas_order', 'coupon');
CALL clas_p3_move_table_back('clas_order', 'user_coupon');
CALL clas_p3_move_table_back('clas_order', 'review');
CALL clas_p3_move_table_back('clas_order', 'review_image');
CALL clas_p3_move_table_back('clas_order', 'review_reply');
CALL clas_p3_move_table_back('clas_order', 'review_vote');
CALL clas_p3_move_table_back('clas_order', 'review_user_hidden');
CALL clas_p3_move_table_back('clas_order', 'deleted_review_backup');
CALL clas_p3_move_table_back('clas_order', 'review_delete_request');

CALL clas_p3_move_table_back('clas_compat', 'announcement');
CALL clas_p3_move_table_back('clas_compat', 'rider_application');
CALL clas_p3_move_table_back('clas_compat', 'rider_profile');
CALL clas_p3_move_table_back('clas_compat', 'rider_audit_log');
CALL clas_p3_move_table_back('clas_compat', 'rider_profile_change_request');
CALL clas_p3_move_table_back('clas_compat', 'rider_location_history');
CALL clas_p3_move_table_back('clas_compat', 'delivery_exception');
CALL clas_p3_move_table_back('clas_compat', 'delivery_call_session');
CALL clas_p3_move_table_back('clas_compat', 'rider_settlement');
CALL clas_p3_move_table_back('clas_compat', 'rider_withdrawal');
CALL clas_p3_move_table_back('clas_compat', 'rider_tip');
CALL clas_p3_move_table_back('clas_compat', 'rider_review');
CALL clas_p3_move_table_back('clas_compat', 'rider_daily_metrics');
CALL clas_p3_move_table_back('clas_compat', 'chat_conversation');
CALL clas_p3_move_table_back('clas_compat', 'chat_message');

DROP PROCEDURE IF EXISTS clas_p3_move_table_back;

-- 表回到同一库后，尽力恢复跨域外键（缺表则跳过）
SET @restore_fk = (
  SELECT IF(
    COUNT(*) = 2,
    'ALTER TABLE clas.order_item ADD CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES clas.product(id)',
    'SELECT 1'
  )
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = 'clas' AND TABLE_TYPE = 'BASE TABLE'
    AND TABLE_NAME IN ('order_item', 'product')
);
SET @fk_exists = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = 'clas' AND TABLE_NAME = 'order_item'
    AND CONSTRAINT_NAME = 'fk_order_item_product'
);
SET @restore_fk = IF(@fk_exists > 0, 'SELECT 1', @restore_fk);
PREPARE stmt FROM @restore_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
