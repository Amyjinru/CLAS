-- CLAS 演示数据：多用户不等量收藏 + 已完成订单好评
-- 目标：仅有 logo 且存在带图商品的 OPEN 商家
-- 幂等：favorite 用 INSERT IGNORE；订单/评价按 user+merchant 是否已有评价跳过

SET NAMES utf8mb4;

-- 1) 补充演示用户（密码明文 Abc123! 的 BCrypt）
INSERT IGNORE INTO `user` (phone, username, password, role, enabled) VALUES
    ('13800000004', 'demo_user_a', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000005', 'demo_user_b', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000006', 'demo_user_c', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000007', 'demo_user_d', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000008', 'demo_user_e', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1);

-- 2) 为演示用户补默认地址（若无）
INSERT INTO user_address (user_id, contact_name, phone, address, longitude, latitude, is_default, created_at, updated_at)
SELECT u.phone, u.username, u.phone, CONCAT('演示地址-', u.username), 116.398000, 39.910000, 1, NOW(), NOW()
FROM `user` u
WHERE u.phone IN ('13800000004','13800000005','13800000006','13800000007','13800000008')
  AND NOT EXISTS (SELECT 1 FROM user_address a WHERE a.user_id = u.phone);

-- 3) 临时表：符合条件的商家
DROP TEMPORARY TABLE IF EXISTS tmp_demo_merchants;
CREATE TEMPORARY TABLE tmp_demo_merchants AS
SELECT
    m.id AS merchant_id,
    m.user_id AS owner_phone,
    m.merchant_name,
    (
        SELECT p.id
        FROM product p
        WHERE p.merchant_id = m.id
          AND p.status = 'ON_SALE'
          AND p.image IS NOT NULL AND TRIM(p.image) <> ''
        ORDER BY p.id
        LIMIT 1
    ) AS product_id,
    (
        SELECT p.price
        FROM product p
        WHERE p.merchant_id = m.id
          AND p.status = 'ON_SALE'
          AND p.image IS NOT NULL AND TRIM(p.image) <> ''
        ORDER BY p.id
        LIMIT 1
    ) AS product_price,
    ROW_NUMBER() OVER (ORDER BY m.id) AS rn
FROM merchant m
WHERE m.status = 'OPEN'
  AND m.logo IS NOT NULL AND TRIM(m.logo) <> ''
  AND EXISTS (
      SELECT 1 FROM product p
      WHERE p.merchant_id = m.id
        AND p.status = 'ON_SALE'
        AND p.image IS NOT NULL AND TRIM(p.image) <> ''
  );

-- 4) 临时表：演示收藏用户
DROP TEMPORARY TABLE IF EXISTS tmp_demo_users;
CREATE TEMPORARY TABLE tmp_demo_users AS
SELECT phone AS user_id, ROW_NUMBER() OVER (ORDER BY phone) AS un
FROM `user`
WHERE role = 'USER'
  AND phone IN ('13800000001','13800000004','13800000005','13800000006','13800000007','13800000008');

-- 5) 不等量收藏：按商家序号分配 1~5 个不同用户
INSERT IGNORE INTO favorite (user_id, merchant_id, created_at)
SELECT u.user_id, m.merchant_id, DATE_SUB(NOW(), INTERVAL (m.rn + u.un) MINUTE)
FROM tmp_demo_merchants m
JOIN tmp_demo_users u
  ON u.un <= ((m.rn - 1) MOD 5) + 1
 AND u.user_id <> m.owner_phone;

-- 6) 为尚无评价的部分用户-商家组合补 COMPLETED 订单 + 好评
DROP TEMPORARY TABLE IF EXISTS tmp_review_plan;
CREATE TEMPORARY TABLE tmp_review_plan AS
SELECT
    m.merchant_id,
    m.owner_phone,
    m.product_id,
    m.product_price,
    u.user_id,
    4 + ((m.rn + u.un) MOD 2) AS review_score,
    CASE ((m.rn + u.un) MOD 7)
        WHEN 0 THEN '味道很棒，和图片一致，配送也很快，会再点。'
        WHEN 1 THEN '包装仔细，分量足，整体体验超出预期。'
        WHEN 2 THEN '服务热情，餐品新鲜，性价比很高。'
        WHEN 3 THEN '第一次点就很满意，招牌商品值得推荐。'
        WHEN 4 THEN '出餐稳定，配送准时，值得收藏。'
        WHEN 5 THEN '环境干净，口味在线，已经推荐给同学。'
        ELSE '整体满意，会作为日常回购店铺。'
    END AS review_content
FROM tmp_demo_merchants m
JOIN tmp_demo_users u
  ON u.un <= ((m.rn + 1) MOD 4) + 2
 AND u.user_id <> m.owner_phone
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    JOIN review r ON r.order_id = o.id
    WHERE o.merchant_id = m.merchant_id
      AND o.user_id = u.user_id
);

-- 6a) 插入订单
INSERT INTO orders (
    user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
    status, delivery_address, delivery_status, estimated_minutes,
    create_time, paid_at, accepted_at, completed_at, remark
)
SELECT
    p.user_id,
    p.merchant_id,
    p.product_price + 300,
    p.product_price,
    300,
    0,
    'COMPLETED',
    '演示自动补单地址',
    'DELIVERED',
    30,
    DATE_SUB(NOW(), INTERVAL (p.merchant_id + LENGTH(p.user_id)) DAY),
    DATE_SUB(NOW(), INTERVAL (p.merchant_id + LENGTH(p.user_id)) DAY),
    DATE_SUB(NOW(), INTERVAL (p.merchant_id + LENGTH(p.user_id)) DAY),
    DATE_SUB(NOW(), INTERVAL (p.merchant_id + LENGTH(p.user_id) - 1) DAY),
    '演示数据自动补单'
FROM tmp_review_plan p;

-- 6b) 插入订单明细（关联刚插入的订单）
INSERT INTO order_item (order_id, product_id, quantity, price)
SELECT o.id, p.product_id, 1, p.product_price
FROM tmp_review_plan p
JOIN orders o
  ON o.user_id = p.user_id
 AND o.merchant_id = p.merchant_id
 AND o.remark = '演示数据自动补单'
 AND o.status = 'COMPLETED'
LEFT JOIN order_item oi ON oi.order_id = o.id
WHERE oi.id IS NULL;

-- 6c) 插入评价
INSERT INTO review (order_id, user_id, score, content, report_status, created_at)
SELECT o.id, p.user_id, p.review_score, p.review_content, 'NONE', o.completed_at
FROM tmp_review_plan p
JOIN orders o
  ON o.user_id = p.user_id
 AND o.merchant_id = p.merchant_id
 AND o.remark = '演示数据自动补单'
LEFT JOIN review r ON r.order_id = o.id
WHERE r.id IS NULL;

-- 7) 回写商家评分
UPDATE merchant m
JOIN (
    SELECT o.merchant_id, ROUND(AVG(r.score), 2) AS avg_score
    FROM review r
    JOIN orders o ON o.id = r.order_id
    GROUP BY o.merchant_id
) s ON s.merchant_id = m.id
SET m.score = LEAST(5.00, s.avg_score),
    m.updated_at = NOW();

-- 8) 汇总输出（避免同一临时表在 UNION 中重复打开）
SELECT 'eligible_merchants' AS metric, COUNT(*) AS value
FROM merchant m
WHERE m.status = 'OPEN'
  AND m.logo IS NOT NULL AND TRIM(m.logo) <> ''
  AND EXISTS (
      SELECT 1 FROM product p
      WHERE p.merchant_id = m.id AND p.status = 'ON_SALE'
        AND p.image IS NOT NULL AND TRIM(p.image) <> ''
  );
