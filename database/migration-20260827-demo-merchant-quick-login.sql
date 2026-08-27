-- 为课程演示补齐商家快捷登录账号；重复执行不会覆盖已有商家资料。
INSERT INTO `user` (phone, username, password, role, enabled, nickname)
SELECT '14000000001', 'merchant_fourteen', '$2b$10$6soFp3xNmeVb45jGFavyjOSXBnswM1lUrAccQrkYildzkqehv6qVS', 'MERCHANT', 1, '演示商家14'
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE phone = '14000000001');

INSERT INTO user_role (user_id, role, status, created_at, updated_at)
SELECT u.phone, 'MERCHANT', 'APPROVED', NOW(), NOW()
FROM `user` u
WHERE u.phone IN ('14000000001', '13345678901')
  AND NOT EXISTS (
      SELECT 1 FROM user_role r
      WHERE r.user_id = u.phone AND r.role = 'MERCHANT'
  );

INSERT INTO merchant (
    user_id, merchant_name, phone, category, address, longitude, latitude,
    delivery_radius_m, business_hours, default_prepare_minutes, delivery_fee,
    min_order_price, average_price, score, status, created_at, updated_at
)
SELECT
    '14000000001', '演示商家14', '14000000001', '课程演示', '北京市海淀区学院路演示商家14', 116.352200, 39.992800,
    3000, '09:00-22:00', 12, 300, 0, 2200, 4.60, 'OPEN', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE user_id = '14000000001');
