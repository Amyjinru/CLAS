-- CLAS 演示数据追加：更多演示用户、收藏与 4~5 星好评
-- 目标：抬高有 logo 的演示商铺在智能推荐中的权重基础（收藏量 + 评价量 + 评分）
-- 幂等：用户 INSERT IGNORE；收藏 INSERT IGNORE；评价按 user+merchant 是否已有评价跳过

SET NAMES utf8mb4;

INSERT IGNORE INTO `user` (phone, username, password, role, enabled) VALUES
    ('13800000009', 'demo_user_f', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000010', 'demo_user_g', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000011', 'demo_user_h', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000012', 'demo_user_i', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000013', 'demo_user_j', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000014', 'demo_user_k', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000015', 'demo_user_l', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000016', 'demo_user_m', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000017', 'demo_user_n', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1),
    ('13800000018', 'demo_user_o', '$2b$10$KNBBNGHb7LzajDdlBAgdvuHQSn4QertbOpY7Y/lgT07RsZ4E545s.', 'USER', 1);

INSERT INTO user_address (user_id, contact_name, phone, address, longitude, latitude, is_default, created_at, updated_at)
SELECT u.phone, u.username, u.phone, CONCAT('演示地址-', u.username), 116.398000, 39.910000, 1, NOW(), NOW()
FROM `user` u
WHERE u.phone BETWEEN '13800000009' AND '13800000018'
  AND NOT EXISTS (SELECT 1 FROM user_address a WHERE a.user_id = u.phone);

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

DROP TEMPORARY TABLE IF EXISTS tmp_extra_users;
CREATE TEMPORARY TABLE tmp_extra_users AS
SELECT phone AS user_id, ROW_NUMBER() OVER (ORDER BY phone) AS un
FROM `user`
WHERE role = 'USER'
  AND phone BETWEEN '13800000001' AND '13800000018';

-- 追加收藏：演示用户 × 演示商铺（跳过店主本人）
INSERT IGNORE INTO favorite (user_id, merchant_id, created_at)
SELECT u.user_id, m.merchant_id, DATE_SUB(NOW(), INTERVAL (m.rn + u.un) MINUTE)
FROM tmp_demo_merchants m
JOIN tmp_extra_users u
  ON u.user_id <> m.owner_phone;

DROP TEMPORARY TABLE IF EXISTS tmp_extra_review_plan;
CREATE TEMPORARY TABLE tmp_extra_review_plan AS
SELECT
    m.merchant_id,
    m.owner_phone,
    m.product_id,
    m.product_price,
    u.user_id,
    4 + ((m.rn + u.un) MOD 2) AS review_score,
    CASE ((m.rn + u.un) MOD 8)
        WHEN 0 THEN '回购多次了，味道稳定，配送也准时。'
        WHEN 1 THEN '招牌菜很惊艳，包装干净，体验很好。'
        WHEN 2 THEN '同学推荐来的，没有踩雷，值得收藏。'
        WHEN 3 THEN '分量足，性价比高，已经加入常点清单。'
        WHEN 4 THEN '服务态度好，出餐快，整体非常满意。'
        WHEN 5 THEN '食材新鲜，口味在线，会再下单。'
        WHEN 6 THEN '环境不错，餐品和图片一致，推荐。'
        ELSE '五星好评，是校园周边很靠谱的一家。'
    END AS review_content
FROM tmp_demo_merchants m
JOIN tmp_extra_users u
  ON u.un <= ((m.rn + 2) MOD 5) + 4
 AND u.user_id <> m.owner_phone
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    JOIN review r ON r.order_id = o.id
    WHERE o.merchant_id = m.merchant_id
      AND o.user_id = u.user_id
);

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
    '演示追加补单地址',
    'DELIVERED',
    28,
    DATE_SUB(NOW(), INTERVAL (p.merchant_id + u.un) DAY),
    DATE_SUB(NOW(), INTERVAL (p.merchant_id + u.un) DAY),
    DATE_SUB(NOW(), INTERVAL (p.merchant_id + u.un) DAY),
    DATE_SUB(NOW(), INTERVAL (p.merchant_id + u.un - 1) DAY),
    '演示数据追加补单'
FROM tmp_extra_review_plan p
JOIN tmp_extra_users u ON u.user_id = p.user_id;

INSERT INTO order_item (order_id, product_id, quantity, price)
SELECT o.id, p.product_id, 1, p.product_price
FROM tmp_extra_review_plan p
JOIN orders o
  ON o.user_id = p.user_id
 AND o.merchant_id = p.merchant_id
 AND o.remark = '演示数据追加补单'
 AND o.status = 'COMPLETED'
LEFT JOIN order_item oi ON oi.order_id = o.id
WHERE oi.id IS NULL;

INSERT INTO review (order_id, user_id, score, content, report_status, created_at)
SELECT o.id, p.user_id, p.review_score, p.review_content, 'NONE', o.completed_at
FROM tmp_extra_review_plan p
JOIN orders o
  ON o.user_id = p.user_id
 AND o.merchant_id = p.merchant_id
 AND o.remark = '演示数据追加补单'
LEFT JOIN review r ON r.order_id = o.id
WHERE r.id IS NULL;

UPDATE merchant m
JOIN (
    SELECT o.merchant_id, ROUND(AVG(r.score), 2) AS avg_score
    FROM review r
    JOIN orders o ON o.id = r.order_id
    GROUP BY o.merchant_id
) s ON s.merchant_id = m.id
SET m.score = LEAST(5.00, s.avg_score),
    m.updated_at = NOW();

SELECT 'extra_reviews_added' AS metric, COUNT(*) AS value
FROM review r
JOIN orders o ON o.id = r.order_id
WHERE o.remark IN ('演示数据自动补单', '演示数据追加补单');

SELECT m.id, m.merchant_name, ROUND(m.score, 2) AS score,
       COUNT(DISTINCT f.user_id) AS fav_users,
       COUNT(DISTINCT r.id) AS reviews
FROM merchant m
LEFT JOIN favorite f ON f.merchant_id = m.id
LEFT JOIN orders o ON o.merchant_id = m.id
LEFT JOIN review r ON r.order_id = o.id
WHERE m.status = 'OPEN'
  AND m.logo IS NOT NULL AND TRIM(m.logo) <> ''
GROUP BY m.id, m.merchant_name, m.score
ORDER BY fav_users DESC, reviews DESC, m.score DESC
LIMIT 12;
