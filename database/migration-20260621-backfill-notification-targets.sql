-- Backfill structured targets for notifications created before target fields existed.
-- Safe to run multiple times: rows with target_type and target_path are left unchanged.

UPDATE notification n
JOIN orders o ON n.content LIKE CONCAT('订单 ', o.id, ' %')
SET n.type = 'ORDER_STATUS',
    n.target_type = 'ORDER',
    n.target_id = o.id,
    n.order_id = o.id,
    n.merchant_id = o.merchant_id,
    n.target_path = CONCAT('/order/', o.id)
WHERE n.title IN ('订单已创建','商家已接单','订单配送中','订单已完成','商家已拒单','退款申请已提交','退款已通过','退款被拒绝')
  AND (n.target_type IS NULL OR n.target_path IS NULL);

UPDATE notification n
JOIN review r ON n.content LIKE CONCAT('您的订单 ', r.order_id, ' 评价%')
JOIN orders o ON o.id = r.order_id
SET n.type = 'MERCHANT_REVIEW_REPLY',
    n.target_type = 'REVIEW',
    n.target_id = r.id,
    n.review_id = r.id,
    n.order_id = r.order_id,
    n.merchant_id = o.merchant_id,
    n.target_path = CONCAT('/review/', r.order_id, '?reviewId=', r.id)
WHERE n.title = '商家回复了评价'
  AND (n.target_type IS NULL OR n.target_path IS NULL);

UPDATE notification n
JOIN service_booking b ON n.user_id = b.user_id
    AND n.content LIKE CONCAT(b.service_name, ' %')
SET n.type = 'BOOKING_STATUS',
    n.target_type = 'BOOKING',
    n.target_id = b.id,
    n.merchant_id = b.merchant_id,
    n.target_path = CONCAT('/bookings?bookingId=', b.id)
WHERE n.title IN ('预约已提交','预约状态更新')
  AND (n.target_type IS NULL OR n.target_path IS NULL);

UPDATE notification n
JOIN merchant m ON n.user_id = m.user_id
JOIN service_booking b ON b.merchant_id = m.id
    AND n.content LIKE CONCAT(b.service_name, ' %')
SET n.type = 'BOOKING_STATUS',
    n.target_type = 'BOOKING',
    n.target_id = b.id,
    n.merchant_id = b.merchant_id,
    n.target_path = CONCAT('/merchant/bookings?bookingId=', b.id)
WHERE n.title IN ('新的预约申请','预约已取消')
  AND (n.target_type IS NULL OR n.target_path IS NULL);

UPDATE notification n
JOIN deal_order d ON n.user_id = d.user_id
    AND n.content LIKE CONCAT('%', d.voucher_code, '%')
SET n.type = 'DEAL_ORDER_STATUS',
    n.target_type = 'DEAL_ORDER',
    n.target_id = d.id,
    n.order_id = d.id,
    n.merchant_id = d.merchant_id,
    n.target_path = CONCAT('/deal-order/', d.id)
WHERE n.title IN ('团购券购买成功','团购券已核销','团购券已退款')
  AND (n.target_type IS NULL OR n.target_path IS NULL);

UPDATE notification n
JOIN deal_order d ON n.user_id = d.user_id
    AND d.id = (
        SELECT MAX(d2.id)
        FROM deal_order d2
        WHERE d2.user_id = n.user_id
          AND (n.created_at IS NULL OR d2.create_time <= DATE_ADD(n.created_at, INTERVAL 2 SECOND))
    )
SET n.type = 'DEAL_ORDER_STATUS',
    n.target_type = 'DEAL_ORDER',
    n.target_id = d.id,
    n.order_id = d.id,
    n.merchant_id = d.merchant_id,
    n.target_path = CONCAT('/deal-order/', d.id)
WHERE n.title = '团购券待支付'
  AND (n.target_type IS NULL OR n.target_path IS NULL);

SELECT COUNT(*) AS total,
       SUM(target_type IS NULL OR target_path IS NULL) AS missing_target
FROM notification;
