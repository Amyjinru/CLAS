-- 回滚 #49 订单写隔离账号。执行后各服务应改回 MYSQL_USER=root 或集群 clas 账号。
-- 不删除 clas 业务数据。

REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'clas_order_app'@'%';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'clas_order_app'@'localhost';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'clas_app'@'%';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'clas_app'@'localhost';

DROP USER IF EXISTS 'clas_order_app'@'%';
DROP USER IF EXISTS 'clas_order_app'@'localhost';
DROP USER IF EXISTS 'clas_app'@'%';
DROP USER IF EXISTS 'clas_app'@'localhost';
DROP DATABASE IF EXISTS clas_order;

FLUSH PRIVILEGES;
