-- Run before adding core relationship constraints. Any non-zero row count needs review.

SELECT 'order_item.order_id' AS relationship, COUNT(*) AS orphan_count
FROM order_item oi
LEFT JOIN orders o ON o.id = oi.order_id
WHERE o.id IS NULL
UNION ALL
SELECT 'order_item.product_id' AS relationship, COUNT(*) AS orphan_count
FROM order_item oi
LEFT JOIN product p ON p.id = oi.product_id
WHERE p.id IS NULL
UNION ALL
SELECT 'payment.order_id' AS relationship, COUNT(*) AS orphan_count
FROM payment p
LEFT JOIN orders o ON o.id = p.order_id
WHERE o.id IS NULL
UNION ALL
SELECT 'review.order_id' AS relationship, COUNT(*) AS orphan_count
FROM review r
LEFT JOIN orders o ON o.id = r.order_id
WHERE o.id IS NULL
UNION ALL
SELECT 'user_coupon.coupon_id' AS relationship, COUNT(*) AS orphan_count
FROM user_coupon uc
LEFT JOIN coupon c ON c.id = uc.coupon_id
WHERE c.id IS NULL;
