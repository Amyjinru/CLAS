-- 2026-06-10：安全加固迁移
-- 1. 密码字段变更为 BCrypt 哈希存储（无 DDL 变更，password VARCHAR(255) 足够容纳 BCrypt 哈希）
-- 2. 旧明文密码通过登录时自动升级机制迁移（UserService.login() 检测 $2 前缀）
-- 3. 如需手动批量升级所有密码，取消注释以下语句（需在 Java 端生成 BCrypt 哈希后填入）：
-- UPDATE `user` SET password = '<BCRYPT_HASH>' WHERE password NOT LIKE '$2%';
--
-- 注意事项：
-- - JWT 密钥通过 jwt.secret 配置项注入
-- - 数据库密码通过 MYSQL_PASSWORD 环境变量注入
-- - CORS 白名单通过 app.cors.allowed-origins 配置

USE clas;

-- 验证 password 列长度足够存放 BCrypt 哈希（BCrypt 输出固定 60 字符）
SELECT phone, CHAR_LENGTH(password) AS pwd_len FROM `user`;
