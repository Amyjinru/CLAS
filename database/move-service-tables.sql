-- #36 P3：把 clas 中的业务表 MOVE 到各服务库。幂等。
-- 必须先跑 isolate-service-privileges.sql（目标库与账号已存在）。
-- 跨域外键 fk_order_item_product 无法跨 schema 保留，删除后由 catalog 内部 API 保证商品存在。

USE clas;

DROP PROCEDURE IF EXISTS clas_p3_drop_cross_fk;
DROP PROCEDURE IF EXISTS clas_p3_move_table;

DELIMITER $$

CREATE PROCEDURE clas_p3_drop_cross_fk()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'clas'
      AND TABLE_NAME = 'order_item'
      AND CONSTRAINT_NAME = 'fk_order_item_product'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  ) THEN
    ALTER TABLE clas.order_item DROP FOREIGN KEY fk_order_item_product;
  END IF;
END$$

CREATE PROCEDURE clas_p3_move_table(IN dest VARCHAR(64), IN tbl VARCHAR(64))
BEGIN
  DECLARE src_cnt INT DEFAULT 0;
  DECLARE dst_cnt INT DEFAULT 0;
  SELECT COUNT(*) INTO src_cnt FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'clas' AND TABLE_NAME = tbl AND TABLE_TYPE = 'BASE TABLE';
  SELECT COUNT(*) INTO dst_cnt FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = dest AND TABLE_NAME = tbl AND TABLE_TYPE = 'BASE TABLE';
  IF src_cnt = 1 AND dst_cnt = 0 THEN
    SET @ddl = CONCAT('RENAME TABLE clas.`', tbl, '` TO `', dest, '`.`', tbl, '`');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  ELSEIF src_cnt = 1 AND dst_cnt = 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'P3 move conflict: table exists in clas and dest';
  END IF;
END$$

DELIMITER ;

CALL clas_p3_drop_cross_fk();

CALL clas_p3_move_table('clas_iam', 'user');
CALL clas_p3_move_table('clas_iam', 'user_role');
CALL clas_p3_move_table('clas_iam', 'user_address');
CALL clas_p3_move_table('clas_iam', 'user_bank_card');
CALL clas_p3_move_table('clas_iam', 'favorite');
CALL clas_p3_move_table('clas_iam', 'role_application');
CALL clas_p3_move_table('clas_iam', 'notification');
CALL clas_p3_move_table('clas_iam', 'user_penalty');
CALL clas_p3_move_table('clas_iam', 'appeal');

CALL clas_p3_move_table('clas_merchant', 'merchant');
CALL clas_p3_move_table('clas_merchant', 'merchant_audit_log');

CALL clas_p3_move_table('clas_catalog', 'product_category');
CALL clas_p3_move_table('clas_catalog', 'product');
CALL clas_p3_move_table('clas_catalog', 'group_deal');
CALL clas_p3_move_table('clas_catalog', 'service_booking');

CALL clas_p3_move_table('clas_order', 'cart');
CALL clas_p3_move_table('clas_order', 'orders');
CALL clas_p3_move_table('clas_order', 'order_item');
CALL clas_p3_move_table('clas_order', 'order_lifecycle_event');
CALL clas_p3_move_table('clas_order', 'order_refund_dispute');
CALL clas_p3_move_table('clas_order', 'payment');
CALL clas_p3_move_table('clas_order', 'deal_order');
CALL clas_p3_move_table('clas_order', 'deal_redeem_log');
CALL clas_p3_move_table('clas_order', 'coupon');
CALL clas_p3_move_table('clas_order', 'user_coupon');
CALL clas_p3_move_table('clas_order', 'review');
CALL clas_p3_move_table('clas_order', 'review_image');
CALL clas_p3_move_table('clas_order', 'review_reply');
CALL clas_p3_move_table('clas_order', 'review_vote');
CALL clas_p3_move_table('clas_order', 'review_user_hidden');
CALL clas_p3_move_table('clas_order', 'deleted_review_backup');
CALL clas_p3_move_table('clas_order', 'review_delete_request');

CALL clas_p3_move_table('clas_compat', 'announcement');
CALL clas_p3_move_table('clas_compat', 'rider_application');
CALL clas_p3_move_table('clas_compat', 'rider_profile');
CALL clas_p3_move_table('clas_compat', 'rider_audit_log');
CALL clas_p3_move_table('clas_compat', 'rider_profile_change_request');
CALL clas_p3_move_table('clas_compat', 'rider_location_history');
CALL clas_p3_move_table('clas_compat', 'delivery_exception');
CALL clas_p3_move_table('clas_compat', 'delivery_call_session');
CALL clas_p3_move_table('clas_compat', 'rider_settlement');
CALL clas_p3_move_table('clas_compat', 'rider_withdrawal');
CALL clas_p3_move_table('clas_compat', 'rider_tip');
CALL clas_p3_move_table('clas_compat', 'rider_review');
CALL clas_p3_move_table('clas_compat', 'rider_daily_metrics');
CALL clas_p3_move_table('clas_compat', 'chat_conversation');
CALL clas_p3_move_table('clas_compat', 'chat_message');

DROP PROCEDURE IF EXISTS clas_p3_drop_cross_fk;
DROP PROCEDURE IF EXISTS clas_p3_move_table;
