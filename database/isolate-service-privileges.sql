-- #36 P3 各服务最小权限：同实例逻辑隔离。
-- 物理表仍在 clas；空库 clas_iam/clas_merchant/clas_catalog/clas_order/clas_compat 留给后续 MOVE。
-- 占位符由 isolate-service-privileges.ps1 替换，禁止提交真实密码。
-- clas_app 改为全表 SELECT-only，由 isolate-service-privileges.ps1 按 information_schema 生成。

CREATE DATABASE IF NOT EXISTS clas_iam
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clas_merchant
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clas_catalog
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clas_order
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clas_compat
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'clas_iam_app'@'%' IDENTIFIED BY '{{CLAS_IAM_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_iam_app'@'localhost' IDENTIFIED BY '{{CLAS_IAM_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_merchant_app'@'%' IDENTIFIED BY '{{CLAS_MERCHANT_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_merchant_app'@'localhost' IDENTIFIED BY '{{CLAS_MERCHANT_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_catalog_app'@'%' IDENTIFIED BY '{{CLAS_CATALOG_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_catalog_app'@'localhost' IDENTIFIED BY '{{CLAS_CATALOG_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_order_app'@'%' IDENTIFIED BY '{{CLAS_ORDER_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_order_app'@'localhost' IDENTIFIED BY '{{CLAS_ORDER_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_compat_app'@'%' IDENTIFIED BY '{{CLAS_COMPAT_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_compat_app'@'localhost' IDENTIFIED BY '{{CLAS_COMPAT_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_app'@'%' IDENTIFIED BY '{{CLAS_APP_PASSWORD}}';
CREATE USER IF NOT EXISTS 'clas_app'@'localhost' IDENTIFIED BY '{{CLAS_APP_PASSWORD}}';

ALTER USER 'clas_iam_app'@'%' IDENTIFIED BY '{{CLAS_IAM_PASSWORD}}';
ALTER USER 'clas_iam_app'@'localhost' IDENTIFIED BY '{{CLAS_IAM_PASSWORD}}';
ALTER USER 'clas_merchant_app'@'%' IDENTIFIED BY '{{CLAS_MERCHANT_PASSWORD}}';
ALTER USER 'clas_merchant_app'@'localhost' IDENTIFIED BY '{{CLAS_MERCHANT_PASSWORD}}';
ALTER USER 'clas_catalog_app'@'%' IDENTIFIED BY '{{CLAS_CATALOG_PASSWORD}}';
ALTER USER 'clas_catalog_app'@'localhost' IDENTIFIED BY '{{CLAS_CATALOG_PASSWORD}}';
ALTER USER 'clas_order_app'@'%' IDENTIFIED BY '{{CLAS_ORDER_PASSWORD}}';
ALTER USER 'clas_order_app'@'localhost' IDENTIFIED BY '{{CLAS_ORDER_PASSWORD}}';
ALTER USER 'clas_compat_app'@'%' IDENTIFIED BY '{{CLAS_COMPAT_PASSWORD}}';
ALTER USER 'clas_compat_app'@'localhost' IDENTIFIED BY '{{CLAS_COMPAT_PASSWORD}}';
ALTER USER 'clas_app'@'%' IDENTIFIED BY '{{CLAS_APP_PASSWORD}}';
ALTER USER 'clas_app'@'localhost' IDENTIFIED BY '{{CLAS_APP_PASSWORD}}';

GRANT ALL PRIVILEGES ON clas_iam.* TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas_iam.* TO 'clas_iam_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.user TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas.user TO 'clas_iam_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.user_role TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas.user_role TO 'clas_iam_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.user_address TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas.user_address TO 'clas_iam_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.user_bank_card TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas.user_bank_card TO 'clas_iam_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.favorite TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas.favorite TO 'clas_iam_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.role_application TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas.role_application TO 'clas_iam_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.notification TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas.notification TO 'clas_iam_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.user_penalty TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas.user_penalty TO 'clas_iam_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.appeal TO 'clas_iam_app'@'%';
GRANT ALL PRIVILEGES ON clas.appeal TO 'clas_iam_app'@'localhost';

GRANT ALL PRIVILEGES ON clas_merchant.* TO 'clas_merchant_app'@'%';
GRANT ALL PRIVILEGES ON clas_merchant.* TO 'clas_merchant_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.merchant TO 'clas_merchant_app'@'%';
GRANT ALL PRIVILEGES ON clas.merchant TO 'clas_merchant_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.merchant_audit_log TO 'clas_merchant_app'@'%';
GRANT ALL PRIVILEGES ON clas.merchant_audit_log TO 'clas_merchant_app'@'localhost';

GRANT ALL PRIVILEGES ON clas_catalog.* TO 'clas_catalog_app'@'%';
GRANT ALL PRIVILEGES ON clas_catalog.* TO 'clas_catalog_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.product_category TO 'clas_catalog_app'@'%';
GRANT ALL PRIVILEGES ON clas.product_category TO 'clas_catalog_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.product TO 'clas_catalog_app'@'%';
GRANT ALL PRIVILEGES ON clas.product TO 'clas_catalog_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.group_deal TO 'clas_catalog_app'@'%';
GRANT ALL PRIVILEGES ON clas.group_deal TO 'clas_catalog_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.service_booking TO 'clas_catalog_app'@'%';
GRANT ALL PRIVILEGES ON clas.service_booking TO 'clas_catalog_app'@'localhost';

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

GRANT ALL PRIVILEGES ON clas_compat.* TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas_compat.* TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.announcement TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.announcement TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_application TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_application TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_profile TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_profile TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_audit_log TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_audit_log TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_profile_change_request TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_profile_change_request TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_location_history TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_location_history TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.delivery_exception TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.delivery_exception TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.delivery_call_session TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.delivery_call_session TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_settlement TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_settlement TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_withdrawal TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_withdrawal TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_tip TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_tip TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_review TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_review TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.rider_daily_metrics TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.rider_daily_metrics TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.chat_conversation TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.chat_conversation TO 'clas_compat_app'@'localhost';
GRANT ALL PRIVILEGES ON clas.chat_message TO 'clas_compat_app'@'%';
GRANT ALL PRIVILEGES ON clas.chat_message TO 'clas_compat_app'@'localhost';

FLUSH PRIVILEGES;
