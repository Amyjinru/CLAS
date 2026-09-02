-- #49 订单域最小权限：同实例逻辑隔离。
-- 物理表仍在 clas（compat Admin/统计/聊天仍 SELECT）。
-- clas_order 为空库，留给 #39/#50 后续 MOVE。
-- 占位符由 isolate-order-privileges.ps1 替换，禁止提交真实密码。
-- clas_app 的逐表 GRANT 由 isolate-order-privileges.ps1 生成（避免 schema 级 ALL + REVOKE 失败）。

CREATE DATABASE IF NOT EXISTS clas_order
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'clas_order_app'@'%' IDENTIFIED BY '{{CLAS_ORDER_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_order_app'@'localhost' IDENTIFIED BY '{{CLAS_ORDER_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_app'@'%' IDENTIFIED BY '{{CLAS_APP_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_app'@'localhost' IDENTIFIED BY '{{CLAS_APP_PASSWORD}}';

ALTER USER 'clas_order_app'@'%' IDENTIFIED BY '{{CLAS_ORDER_PASSWORD}}';
ALTER USER 'clas_order_app'@'localhost' IDENTIFIED BY '{{CLAS_ORDER_PASSWORD}}';
ALTER USER 'clas_app'@'%' IDENTIFIED BY '{{CLAS_APP_PASSWORD}}';
ALTER USER 'clas_app'@'localhost' IDENTIFIED BY '{{CLAS_APP_PASSWORD}}';

GRANT ALL PRIVILEGES ON clas_order.* TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas_order.* TO 'clas_order_app'@'localhost';

GRANT ALL PRIVILEGES ON clas.cart TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.cart TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.orders TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.orders TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.order_item TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.order_item TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.order_lifecycle_event TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.order_lifecycle_event TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.order_refund_dispute TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.order_refund_dispute TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.payment TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.payment TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.deal_order TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.deal_order TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.deal_redeem_log TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.deal_redeem_log TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.coupon TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.coupon TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.user_coupon TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.user_coupon TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.review TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.review TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.review_image TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.review_image TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.review_reply TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.review_reply TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.review_vote TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.review_vote TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.review_user_hidden TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.review_user_hidden TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.deleted_review_backup TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.deleted_review_backup TO 'clas_order_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.review_delete_request TO 'clas_order_app'@'%';
GRANT ALL PRIVILEGES ON clas.review_delete_request TO 'clas_order_app'@'localhost';

FLUSH PRIVILEGES;
