-- 将种子用户的明文密码升级为 BCrypt 哈希
-- 仅更新仍为明文的密码（不以 $2 开头）
USE clas;

UPDATE user SET password = '$2b$10$ebklc2qPDOxwFCclQs8Ry.PrtzWoJhmas3t/eeREPx4OWi9Bm5ryC'
WHERE password NOT LIKE '$2%' AND password != '';
