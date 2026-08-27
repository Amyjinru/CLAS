-- CLAS 骑手联调补充数据（增量、可重复执行）
-- 仅更新/新增 RIDER_DEMO_13345678900_ 前缀的演示订单和 1334567890x 演示账号。
-- 北航学院路校区附近坐标：商家 116.348854,39.981781；配送范围均在五公里内。

-- 历史表使用 utf8mb4_unicode_ci；显式指定连接排序规则，避免不同 MySQL 默认排序规则比较账号时失败。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @demo_user_id = '13345678900';
SET @demo_rider_id = '13345678903';
SET @demo_peer_rider_id = '13345678904';
SET @demo_merchant_id = (SELECT id FROM merchant WHERE user_id = '13345678901' LIMIT 1);

-- 让已有基础数据迁移到北航学院路校区，不改动任何非演示商家或地址。
UPDATE merchant
SET merchant_name = '北航学院路骑手联调餐厅',
    address = '北京市海淀区学院路37号，北京航空航天大学学院路校区东门附近',
    longitude = 116.348854,
    latitude = 39.981781,
    delivery_radius_m = 5000,
    default_prepare_minutes = 12,
    updated_at = NOW()
WHERE id = @demo_merchant_id;

UPDATE user_address
SET address = '北京市海淀区学院路37号，北京航空航天大学学院路校区新北区',
    longitude = 116.352930,
    latitude = 39.986120,
    updated_at = NOW()
WHERE user_id = @demo_user_id
  AND address LIKE '%演示收货点%';

UPDATE orders
SET delivery_address = '北京市海淀区学院路37号，北京航空航天大学学院路校区新北区',
    delivery_longitude = 116.352930,
    delivery_latitude = 39.986120
WHERE remark LIKE 'RIDER_DEMO_13345678900_%';

-- 主演示骑手保持在线、接单中；另一骑手在线但不接单，用于位置与抢单边界演示。
UPDATE rider_profile
SET online_status = CASE WHEN user_id = @demo_rider_id THEN 1 ELSE 1 END,
    accepting_orders = CASE WHEN user_id = @demo_rider_id THEN 1 ELSE 0 END,
    current_longitude = CASE WHEN user_id = @demo_rider_id THEN 116.349420 ELSE 116.344870 END,
    current_latitude = CASE WHEN user_id = @demo_rider_id THEN 39.983110 ELSE 39.987020 END,
    location_updated_at = NOW(),
    updated_at = NOW()
WHERE user_id IN (@demo_rider_id, @demo_peer_rider_id);

INSERT INTO rider_location_history (rider_id, longitude, latitude, accuracy_meters, reported_at)
SELECT @demo_rider_id, 116.349420, 39.983110, 18, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM rider_location_history
    WHERE rider_id = @demo_rider_id AND longitude = 116.349420 AND latitude = 39.983110
);

-- 将基础订单保留为第一张可接任务，并新增不同佣金/时间的任务，便于验证三种排序方式。
INSERT INTO orders (
    user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
    status, delivery_address, delivery_longitude, delivery_latitude, delivery_status,
    estimated_minutes, prepare_minutes_snapshot, promise_start_at, promise_end_at,
    predicted_arrival_at, rider_commission, create_time, paid_at, accepted_at, remark
)
SELECT @demo_user_id, @demo_merchant_id, 3500, 3200, 300, 0,
    'ACCEPTED', '北京市海淀区学院路37号，北京航空航天大学学院路校区主楼', 116.350240, 39.983590, 'AVAILABLE',
    26, 12, NOW(), DATE_ADD(NOW(), INTERVAL 22 MINUTE), DATE_ADD(NOW(), INTERVAL 19 MINUTE),
    360, NOW(), NOW(), NOW(), 'RIDER_DEMO_13345678900_AVAILABLE_HIGH_COMMISSION'
WHERE @demo_merchant_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders WHERE remark = 'RIDER_DEMO_13345678900_AVAILABLE_HIGH_COMMISSION');

INSERT INTO orders (
    user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
    status, delivery_address, delivery_longitude, delivery_latitude, delivery_status,
    estimated_minutes, prepare_minutes_snapshot, promise_start_at, promise_end_at,
    predicted_arrival_at, rider_commission, create_time, paid_at, accepted_at, remark
)
SELECT @demo_user_id, @demo_merchant_id, 2800, 2500, 300, 0,
    'ACCEPTED', '北京市海淀区学院路37号，北京航空航天大学学院路校区逸夫科学馆', 116.346970, 39.985160, 'AVAILABLE',
    20, 12, NOW(), DATE_ADD(NOW(), INTERVAL 16 MINUTE), DATE_ADD(NOW(), INTERVAL 14 MINUTE),
    180, NOW(), NOW(), NOW(), 'RIDER_DEMO_13345678900_AVAILABLE_NEAREST'
WHERE @demo_merchant_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders WHERE remark = 'RIDER_DEMO_13345678900_AVAILABLE_NEAREST');

INSERT INTO orders (
    user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
    status, delivery_address, delivery_longitude, delivery_latitude, delivery_status,
    estimated_minutes, prepare_minutes_snapshot, promise_start_at, promise_end_at,
    predicted_arrival_at, rider_commission, create_time, paid_at, accepted_at, remark
)
SELECT @demo_user_id, @demo_merchant_id, 3100, 2800, 300, 0,
    'ACCEPTED', '北京市海淀区学院路37号，北京航空航天大学学院路校区体育馆', 116.345910, 39.981210, 'AVAILABLE',
    24, 12, NOW(), DATE_ADD(NOW(), INTERVAL 28 MINUTE), DATE_ADD(NOW(), INTERVAL 23 MINUTE),
    260, NOW(), NOW(), NOW(), 'RIDER_DEMO_13345678900_AVAILABLE_SMART_ROUTE'
WHERE @demo_merchant_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders WHERE remark = 'RIDER_DEMO_13345678900_AVAILABLE_SMART_ROUTE');

-- 第二张在途订单：与“待出餐”订单一起展示多单配送和紧急排序。
INSERT INTO orders (
    user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
    status, delivery_address, delivery_longitude, delivery_latitude, delivery_status, rider_id,
    estimated_minutes, prepare_minutes_snapshot, promise_start_at, promise_end_at,
    predicted_arrival_at, rider_commission, create_time, paid_at, accepted_at, rider_assigned_at,
    picked_up_at, remark
)
SELECT @demo_user_id, @demo_merchant_id, 4200, 3900, 300, 0,
    'ACCEPTED', '北京市海淀区学院路37号，北京航空航天大学学院路校区学生宿舍区', 116.354180, 39.987060, 'DELIVERING', @demo_rider_id,
    30, 12, DATE_SUB(NOW(), INTERVAL 18 MINUTE), DATE_ADD(NOW(), INTERVAL 5 MINUTE), DATE_ADD(NOW(), INTERVAL 4 MINUTE),
    420, DATE_SUB(NOW(), INTERVAL 18 MINUTE), DATE_SUB(NOW(), INTERVAL 18 MINUTE), DATE_SUB(NOW(), INTERVAL 16 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE),
    DATE_SUB(NOW(), INTERVAL 8 MINUTE), 'RIDER_DEMO_13345678900_DELIVERING_URGENT'
WHERE @demo_merchant_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders WHERE remark = 'RIDER_DEMO_13345678900_DELIVERING_URGENT');

-- 一张已完成但尚未评价/打赏的订单，便于在网站上手工验证用户操作。
INSERT INTO orders (
    user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
    status, delivery_address, delivery_longitude, delivery_latitude, delivery_status, rider_id,
    estimated_minutes, prepare_minutes_snapshot, promise_start_at, promise_end_at,
    predicted_arrival_at, rider_commission, create_time, paid_at, accepted_at, rider_assigned_at,
    picked_up_at, delivery_completed_at, delivered_at, completed_at, remark
)
SELECT @demo_user_id, @demo_merchant_id, 3300, 3000, 300, 0,
    'COMPLETED', '北京市海淀区学院路37号，北京航空航天大学学院路校区教学区', 116.350740, 39.979850, 'DELIVERED', @demo_rider_id,
    25, 12, DATE_SUB(NOW(), INTERVAL 100 MINUTE), DATE_SUB(NOW(), INTERVAL 80 MINUTE), DATE_SUB(NOW(), INTERVAL 82 MINUTE),
    280, DATE_SUB(NOW(), INTERVAL 100 MINUTE), DATE_SUB(NOW(), INTERVAL 100 MINUTE), DATE_SUB(NOW(), INTERVAL 95 MINUTE), DATE_SUB(NOW(), INTERVAL 92 MINUTE),
    DATE_SUB(NOW(), INTERVAL 88 MINUTE), DATE_SUB(NOW(), INTERVAL 82 MINUTE), DATE_SUB(NOW(), INTERVAL 80 MINUTE), DATE_SUB(NOW(), INTERVAL 78 MINUTE), 'RIDER_DEMO_13345678900_COMPLETED_READY_FOR_REVIEW'
WHERE @demo_merchant_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders WHERE remark = 'RIDER_DEMO_13345678900_COMPLETED_READY_FOR_REVIEW');

SET @demo_product_id = (SELECT id FROM product WHERE merchant_id = @demo_merchant_id AND name = '骑手联调套餐' LIMIT 1);
INSERT INTO order_item (order_id, product_id, quantity, price)
SELECT o.id, @demo_product_id, 1, o.subtotal
FROM orders o
WHERE o.remark IN (
    'RIDER_DEMO_13345678900_AVAILABLE_HIGH_COMMISSION',
    'RIDER_DEMO_13345678900_AVAILABLE_NEAREST',
    'RIDER_DEMO_13345678900_AVAILABLE_SMART_ROUTE',
    'RIDER_DEMO_13345678900_DELIVERING_URGENT',
    'RIDER_DEMO_13345678900_COMPLETED_READY_FOR_REVIEW'
)
  AND @demo_product_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM order_item i WHERE i.order_id = o.id AND i.product_id = @demo_product_id);

SET @assigned_order_id = (SELECT id FROM orders WHERE remark = 'RIDER_DEMO_13345678900_ASSIGNED' LIMIT 1);
SET @delivering_order_id = (SELECT id FROM orders WHERE remark = 'RIDER_DEMO_13345678900_DELIVERING_URGENT' LIMIT 1);
SET @completed_order_id = (SELECT id FROM orders WHERE remark = 'RIDER_DEMO_13345678900_COMPLETED' LIMIT 1);
SET @review_ready_order_id = (SELECT id FROM orders WHERE remark = 'RIDER_DEMO_13345678900_COMPLETED_READY_FOR_REVIEW' LIMIT 1);

-- 用户—骑手消息仅放在配送中的订单；用户—商家消息用于展示备餐沟通。
INSERT INTO chat_conversation (order_id, conversation_type, user_id, peer_id, last_message_at, created_at)
SELECT @assigned_order_id, 'USER_RIDER', @demo_user_id, @demo_rider_id, NOW(), DATE_SUB(NOW(), INTERVAL 9 MINUTE)
WHERE @assigned_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM chat_conversation WHERE order_id = @assigned_order_id AND conversation_type = 'USER_RIDER');

INSERT INTO chat_conversation (order_id, conversation_type, user_id, peer_id, last_message_at, created_at)
SELECT @delivering_order_id, 'USER_RIDER', @demo_user_id, @demo_rider_id, NOW(), DATE_SUB(NOW(), INTERVAL 6 MINUTE)
WHERE @delivering_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM chat_conversation WHERE order_id = @delivering_order_id AND conversation_type = 'USER_RIDER');

INSERT INTO chat_message (order_id, conversation_type, merchant_id, user_id, rider_id, sender_role, content, created_at)
SELECT @assigned_order_id, 'USER_RIDER', NULL, @demo_user_id, @demo_rider_id, 'USER', '我在北航学院路校区新北区，请到楼下后联系我。', DATE_SUB(NOW(), INTERVAL 9 MINUTE)
WHERE @assigned_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM chat_message WHERE order_id = @assigned_order_id AND conversation_type = 'USER_RIDER' AND sender_role = 'USER');

INSERT INTO chat_message (order_id, conversation_type, merchant_id, user_id, rider_id, sender_role, content, created_at)
SELECT @assigned_order_id, 'USER_RIDER', NULL, @demo_user_id, @demo_rider_id, 'RIDER', '已到店等待出餐，取餐后会尽快配送。', DATE_SUB(NOW(), INTERVAL 7 MINUTE)
WHERE @assigned_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM chat_message WHERE order_id = @assigned_order_id AND conversation_type = 'USER_RIDER' AND sender_role = 'RIDER');

INSERT INTO chat_message (order_id, conversation_type, merchant_id, user_id, rider_id, sender_role, content, created_at)
SELECT @delivering_order_id, 'USER_RIDER', NULL, @demo_user_id, @demo_rider_id, 'RIDER', '已取餐，正在前往学院路校区学生宿舍区，预计几分钟后到达。', DATE_SUB(NOW(), INTERVAL 6 MINUTE)
WHERE @delivering_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM chat_message WHERE order_id = @delivering_order_id AND conversation_type = 'USER_RIDER' AND sender_role = 'RIDER');

INSERT INTO chat_message (order_id, conversation_type, merchant_id, user_id, rider_id, sender_role, content, created_at)
SELECT @assigned_order_id, 'USER_MERCHANT', @demo_merchant_id, @demo_user_id, NULL, 'MERCHANT', '餐品正在制作，预计还需约 6 分钟，骑手已在店等待。', DATE_SUB(NOW(), INTERVAL 10 MINUTE)
WHERE @assigned_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM chat_message WHERE order_id = @assigned_order_id AND conversation_type = 'USER_MERCHANT' AND sender_role = 'MERCHANT');

-- 为已完成订单提供可展示的评价、打赏、收入、提现和每日表现；保留另一已完成订单给页面手工评价/打赏。
INSERT INTO rider_review (order_id, user_id, rider_id, score, tags, content, created_at)
SELECT @completed_order_id, @demo_user_id, @demo_rider_id, 5, '准时送达,沟通友好,路线熟悉', '骑手沟通及时，配送到北航学院路校区很快。', DATE_SUB(NOW(), INTERVAL 17 MINUTE)
WHERE @completed_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rider_review WHERE order_id = @completed_order_id);

INSERT INTO rider_tip (order_id, user_id, rider_id, amount, idempotency_key, status, paid_at)
SELECT @completed_order_id, @demo_user_id, @demo_rider_id, 300, 'RIDER_DEMO_TIP_13345678900', 'PAID', DATE_SUB(NOW(), INTERVAL 17 MINUTE)
WHERE @completed_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rider_tip WHERE order_id = @completed_order_id);

INSERT INTO rider_settlement (rider_id, order_id, source_type, source_id, settlement_type, amount, balance_type, created_at)
SELECT @demo_rider_id, @completed_order_id, 'DEMO_COMMISSION', CONCAT('DEMO_COMMISSION_', @completed_order_id), 'COMMISSION', 200, 'WITHDRAWABLE', DATE_SUB(NOW(), INTERVAL 18 MINUTE)
WHERE @completed_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rider_settlement WHERE source_type = 'DEMO_COMMISSION' AND source_id = CONCAT('DEMO_COMMISSION_', @completed_order_id));

INSERT INTO rider_settlement (rider_id, order_id, source_type, source_id, settlement_type, amount, balance_type, created_at)
SELECT @demo_rider_id, @completed_order_id, 'DEMO_TIP', CONCAT('DEMO_TIP_', @completed_order_id), 'TIP', 300, 'WITHDRAWABLE', DATE_SUB(NOW(), INTERVAL 17 MINUTE)
WHERE @completed_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rider_settlement WHERE source_type = 'DEMO_TIP' AND source_id = CONCAT('DEMO_TIP_', @completed_order_id));

INSERT INTO rider_settlement (rider_id, order_id, source_type, source_id, settlement_type, amount, balance_type, created_at)
SELECT @demo_rider_id, @review_ready_order_id, 'DEMO_COMMISSION', CONCAT('DEMO_COMMISSION_', @review_ready_order_id), 'COMMISSION', 280, 'WITHDRAWABLE', DATE_SUB(NOW(), INTERVAL 78 MINUTE)
WHERE @review_ready_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rider_settlement WHERE source_type = 'DEMO_COMMISSION' AND source_id = CONCAT('DEMO_COMMISSION_', @review_ready_order_id));

INSERT INTO user_bank_card (user_id, bank_name, cardholder_name, card_no_encrypted, card_last4, card_type, is_default, create_time)
SELECT @demo_rider_id, '课程演示银行', '配送测试骑手一', 'demo-only-card-token-8301', '8301', '借记卡', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_bank_card WHERE user_id = @demo_rider_id AND card_last4 = '8301');

SET @demo_card_id = (SELECT id FROM user_bank_card WHERE user_id = @demo_rider_id AND card_last4 = '8301' ORDER BY id DESC LIMIT 1);
INSERT INTO rider_withdrawal (rider_id, bank_card_id, amount, status, reviewer_id, review_reason, created_at, reviewed_at)
SELECT @demo_rider_id, @demo_card_id, 200, 'APPROVED', '13345678902', '课程演示：已审核到账', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE @demo_card_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rider_withdrawal WHERE rider_id = @demo_rider_id AND amount = 200 AND status = 'APPROVED');

INSERT INTO rider_withdrawal (rider_id, bank_card_id, amount, status, reviewer_id, review_reason, created_at, reviewed_at)
SELECT @demo_rider_id, @demo_card_id, 80, 'PENDING', NULL, NULL, DATE_SUB(NOW(), INTERVAL 8 MINUTE), NULL
WHERE @demo_card_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rider_withdrawal WHERE rider_id = @demo_rider_id AND amount = 80 AND status = 'PENDING');

INSERT INTO rider_withdrawal (rider_id, bank_card_id, amount, status, reviewer_id, review_reason, created_at, reviewed_at)
SELECT @demo_rider_id, @demo_card_id, 120, 'REJECTED', '13345678902', '课程演示：银行卡信息待补充，金额已退回', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @demo_card_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rider_withdrawal WHERE rider_id = @demo_rider_id AND amount = 120 AND status = 'REJECTED');

INSERT INTO delivery_exception (order_id, rider_id, exception_type, status, score_deduction, commission_deduction, detail, created_at)
SELECT @completed_order_id, @demo_rider_id, 'OVERDUE', 'CLOSED', 5, 40, '课程演示：历史订单超时扣分与佣金扣减样例', DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @completed_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM delivery_exception WHERE order_id = @completed_order_id AND exception_type = 'OVERDUE');

INSERT INTO rider_settlement (rider_id, order_id, source_type, source_id, settlement_type, amount, balance_type, created_at)
SELECT @demo_rider_id, @completed_order_id, 'DEMO_OVERDUE', CONCAT('DEMO_OVERDUE_', @completed_order_id), 'OVERDUE_DEDUCTION', -40, 'WITHDRAWABLE', DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @completed_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rider_settlement WHERE source_type = 'DEMO_OVERDUE' AND source_id = CONCAT('DEMO_OVERDUE_', @completed_order_id));

UPDATE rider_profile
SET withdrawable_balance = 500,
    frozen_balance = 80,
    updated_at = NOW()
WHERE user_id = @demo_rider_id;

INSERT INTO rider_daily_metrics (rider_id, metric_date, completed_orders, net_income, average_rating, overdue_count, average_delivery_minutes, base_score, manual_adjustment, final_score, grade, archived_at)
SELECT @demo_rider_id, CURDATE(), 2, 740, 5.00, 0, 26, 89.00, 0.00, 89.00, 'B', NOW()
WHERE NOT EXISTS (SELECT 1 FROM rider_daily_metrics WHERE rider_id = @demo_rider_id AND metric_date = CURDATE());

INSERT INTO rider_daily_metrics (rider_id, metric_date, completed_orders, net_income, average_rating, overdue_count, average_delivery_minutes, base_score, manual_adjustment, final_score, grade, archived_at)
SELECT @demo_rider_id, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 3, 980, 4.67, 1, 31, 82.00, 0.00, 77.00, 'C', DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM rider_daily_metrics WHERE rider_id = @demo_rider_id AND metric_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY));

INSERT INTO notification (user_id, title, content, read_flag, type, target_type, order_id, merchant_id, target_path, created_at)
SELECT @demo_rider_id, '新配送任务', '北航学院路校区附近有新的可接任务，可按佣金、距离或智能排序查看。', 0, 'RIDER_TASK', 'ORDER', NULL, @demo_merchant_id, '/rider', NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE user_id = @demo_rider_id AND type = 'RIDER_TASK' AND content LIKE '北航学院路校区附近%');

SELECT 'demo_available_orders' AS metric, COUNT(*) AS value FROM orders WHERE remark LIKE 'RIDER_DEMO_13345678900_AVAILABLE%'
UNION ALL SELECT 'demo_active_orders', COUNT(*) FROM orders WHERE rider_id = @demo_rider_id AND delivery_status IN ('ASSIGNED_WAITING_MEAL', 'DELIVERING')
UNION ALL SELECT 'demo_reviews', COUNT(*) FROM rider_review WHERE rider_id = @demo_rider_id
UNION ALL SELECT 'demo_rider_messages', COUNT(*) FROM chat_message WHERE rider_id = @demo_rider_id AND conversation_type = 'USER_RIDER'
UNION ALL SELECT 'demo_settlements', COUNT(*) FROM rider_settlement WHERE rider_id = @demo_rider_id
UNION ALL SELECT 'demo_withdrawals', COUNT(*) FROM rider_withdrawal WHERE rider_id = @demo_rider_id;
