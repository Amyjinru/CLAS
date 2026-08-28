-- 为已存在的课程演示账号补齐骑手身份；不创建真实用户，也不覆盖已有骑手资料。
INSERT INTO user_role (user_id, role, status, created_at, updated_at)
SELECT u.phone, 'RIDER', 'APPROVED', NOW(), NOW()
FROM `user` u
WHERE u.phone IN ('13800000008', '13345678903')
  AND NOT EXISTS (
      SELECT 1 FROM user_role r
      WHERE r.user_id = u.phone AND r.role = 'RIDER'
  );
