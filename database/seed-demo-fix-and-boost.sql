-- 修复非法评价分并重算商家评分（系统为 5 分制）
UPDATE review
SET score = LEAST(5, GREATEST(1, score))
WHERE score > 5 OR score < 1;

UPDATE merchant m
JOIN (
    SELECT o.merchant_id, ROUND(AVG(r.score), 2) AS avg_score
    FROM review r
    JOIN orders o ON o.id = r.order_id
    GROUP BY o.merchant_id
) s ON s.merchant_id = m.id
SET m.score = LEAST(5.00, s.avg_score),
    m.updated_at = NOW();

-- 演示账号昵称
UPDATE `user` SET username = '小周爱吃' WHERE phone = '13800000004';
UPDATE `user` SET username = '阿宁探店' WHERE phone = '13800000005';
UPDATE `user` SET username = '咖啡控Leo' WHERE phone = '13800000006';
UPDATE `user` SET username = '夜宵选手' WHERE phone = '13800000007';
UPDATE `user` SET username = '校园美食家' WHERE phone = '13800000008';
