-- 回滚 #36 P3 账号。必须先跑 rollback-move-service-tables.sql，把表搬回 clas。
-- 不删除 clas 业务数据；不删除 clas_order / clas_order_app / clas_app。
-- DROP DATABASE 仅删除此时已空的私有库。

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
