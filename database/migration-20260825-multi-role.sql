-- 一个手机号可保留 USER 并获得已审核的商家/骑手身份；切换端口仅改变当前 JWT。
USE clas;

CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role),
    INDEX idx_user_role_role (role)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO user_role (user_id, role)
SELECT phone, 'USER' FROM `user`;
INSERT IGNORE INTO user_role (user_id, role)
SELECT phone, role FROM `user` WHERE role <> 'USER';
