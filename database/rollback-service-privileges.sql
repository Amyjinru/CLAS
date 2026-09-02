-- 回滚 #36 P3 各服务写隔离账号。不删除 clas 业务数据，也不删除 #49 的 clas_order / clas_order_app / clas_app。
-- DROP USER 会带走授权。回滚后 clas_app 可能仍是 SELECT-only；若只要 #49 口径，再跑 isolate-order-privileges.ps1。

DROP USER IF EXISTS 'clas_iam_app'@'%';
DROP USER IF EXISTS 'clas_iam_app'@'localhost';
DROP USER IF EXISTS 'clas_merchant_app'@'%';
DROP USER IF EXISTS 'clas_merchant_app'@'localhost';
DROP USER IF EXISTS 'clas_catalog_app'@'%';
DROP USER IF EXISTS 'clas_catalog_app'@'localhost';
DROP USER IF EXISTS 'clas_compat_app'@'%';
DROP USER IF EXISTS 'clas_compat_app'@'localhost';

DROP DATABASE IF EXISTS clas_iam;
DROP DATABASE IF EXISTS clas_merchant;
DROP DATABASE IF EXISTS clas_catalog;
DROP DATABASE IF EXISTS clas_compat;

FLUSH PRIVILEGES;
