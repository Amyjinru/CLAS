-- CLAS 骑手联调数据（增量、可重复执行）
--
-- 用途：为云端演示提供独立的用户、商家、管理员、两名骑手及配送订单。
-- 安全：仅写入下方 5 个 139000090xx 演示账号，不修改其他业务数据。
-- 密码：Abc123!（仅用于课程演示；生产环境不得使用此类固定账号）。
-- 骑手档案由 scripts/seed-rider-demo.sh 调用正式申请/审核 API 创建，避免在 SQL 中存放身份证明文或伪造密文。

SET NAMES utf8mb4;

INSERT INTO `user` (phone, username, password, role, enabled, nickname)
VALUES
    ('13900009001', 'delivery_demo_user', '$2b$10$6soFp3xNmeVb45jGFavyjOSXBnswM1lUrAccQrkYildzkqehv6qVS', 'USER', 1, '配送测试用户'),
    ('13900009002', 'delivery_demo_merchant', '$2b$10$6soFp3xNmeVb45jGFavyjOSXBnswM1lUrAccQrkYildzkqehv6qVS', 'MERCHANT', 1, '配送测试商家'),
    ('13900009003', 'delivery_demo_admin', '$2b$10$6soFp3xNmeVb45jGFavyjOSXBnswM1lUrAccQrkYildzkqehv6qVS', 'ADMIN', 1, '配送测试管理员'),
    ('13900009004', 'delivery_demo_rider_a', '$2b$10$6soFp3xNmeVb45jGFavyjOSXBnswM1lUrAccQrkYildzkqehv6qVS', 'USER', 1, '配送测试骑手一'),
    ('13900009005', 'delivery_demo_rider_b', '$2b$10$6soFp3xNmeVb45jGFavyjOSXBnswM1lUrAccQrkYildzkqehv6qVS', 'USER', 1, '配送测试骑手二')
ON DUPLICATE KEY UPDATE
    username = VALUES(username), password = VALUES(password), role = VALUES(role), enabled = 1, nickname = VALUES(nickname);

INSERT INTO user_role (user_id, role, status, created_at, updated_at)
SELECT '13900009001', 'USER', 'APPROVED', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_role WHERE user_id = '13900009001' AND role = 'USER');
INSERT INTO user_role (user_id, role, status, created_at, updated_at)
SELECT '13900009002', 'MERCHANT', 'APPROVED', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_role WHERE user_id = '13900009002' AND role = 'MERCHANT');
INSERT INTO user_role (user_id, role, status, created_at, updated_at)
SELECT '13900009003', 'ADMIN', 'APPROVED', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_role WHERE user_id = '13900009003' AND role = 'ADMIN');
INSERT INTO user_role (user_id, role, status, created_at, updated_at)
SELECT '13900009004', 'USER', 'APPROVED', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_role WHERE user_id = '13900009004' AND role = 'USER');
INSERT INTO user_role (user_id, role, status, created_at, updated_at)
SELECT '13900009005', 'USER', 'APPROVED', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_role WHERE user_id = '13900009005' AND role = 'USER');

INSERT INTO merchant (
    user_id, merchant_name, phone, category, address, longitude, latitude,
    delivery_radius_m, business_hours, default_prepare_minutes, delivery_fee,
    min_order_price, average_price, score, status, created_at, updated_at
)
SELECT
    '13900009002', '骑手联调餐厅', '13900009002', '课程演示', '北京市东城区演示配送点', 116.397428, 39.909230,
    5000, '09:00-22:00', 12, 300, 0, 2600, 4.80, 'OPEN', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE user_id = '13900009002');

SET @demo_merchant_id = (SELECT id FROM merchant WHERE user_id = '13900009002' LIMIT 1);

INSERT INTO product (merchant_id, name, description, price, stock, status, created_at, updated_at)
SELECT @demo_merchant_id, '骑手联调套餐', '用于骑手取单、配送和评分验收的演示餐品', 2600, 99, 'ON_SALE', NOW(), NOW()
WHERE @demo_merchant_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM product WHERE merchant_id = @demo_merchant_id AND name = '骑手联调套餐');

SET @demo_product_id = (SELECT id FROM product WHERE merchant_id = @demo_merchant_id AND name = '骑手联调套餐' LIMIT 1);

INSERT INTO user_address (user_id, contact_name, phone, address, longitude, latitude, is_default, created_at, updated_at)
SELECT '13900009001', '配送测试用户', '13900009001', '北京市东城区演示收货点', 116.398000, 39.910000, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_address WHERE user_id = '13900009001' AND address = '北京市东城区演示收货点');

-- 可领取订单：骑手开始接单后，在同一五公里范围内应能看到它。
INSERT INTO orders (
    user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
    status, delivery_address, delivery_longitude, delivery_latitude, delivery_status,
    estimated_minutes, prepare_minutes_snapshot, promise_start_at, promise_end_at,
    predicted_arrival_at, rider_commission, create_time, paid_at, accepted_at, remark
)
SELECT
    '13900009001', @demo_merchant_id, 2900, 2600, 300, 0,
    'ACCEPTED', '北京市东城区演示收货点', 116.398000, 39.910000, 'AVAILABLE',
    30, 12, NOW(), DATE_ADD(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 18 MINUTE),
    200, NOW(), NOW(), NOW(), 'RIDER_DEMO_AVAILABLE'
WHERE @demo_merchant_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders WHERE remark = 'RIDER_DEMO_AVAILABLE');

-- 配送中订单：用于验证骑手—用户联系入口与“结束接单不影响已接订单”。
INSERT INTO orders (
    user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
    status, delivery_address, delivery_longitude, delivery_latitude, delivery_status, rider_id,
    estimated_minutes, prepare_minutes_snapshot, promise_start_at, promise_end_at,
    predicted_arrival_at, rider_commission, create_time, paid_at, accepted_at, rider_assigned_at, remark
)
SELECT
    '13900009001', @demo_merchant_id, 2900, 2600, 300, 0,
    'ACCEPTED', '北京市东城区演示收货点', 116.398000, 39.910000, 'ASSIGNED_WAITING_MEAL', '13900009004',
    30, 12, NOW(), DATE_ADD(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 18 MINUTE),
    200, NOW(), NOW(), NOW(), NOW(), 'RIDER_DEMO_ASSIGNED'
WHERE @demo_merchant_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders WHERE remark = 'RIDER_DEMO_ASSIGNED');

-- 已完成订单：用于验证确认收货后的骑手评价和小费接口。
INSERT INTO orders (
    user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
    status, delivery_address, delivery_longitude, delivery_latitude, delivery_status, rider_id,
    estimated_minutes, prepare_minutes_snapshot, promise_start_at, promise_end_at,
    predicted_arrival_at, rider_commission, create_time, paid_at, accepted_at, rider_assigned_at,
    picked_up_at, delivery_completed_at, delivered_at, completed_at, remark
)
SELECT
    '13900009001', @demo_merchant_id, 2900, 2600, 300, 0,
    'COMPLETED', '北京市东城区演示收货点', 116.398000, 39.910000, 'DELIVERED', '13900009004',
    30, 12, DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 22 MINUTE),
    200, DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 38 MINUTE), DATE_SUB(NOW(), INTERVAL 35 MINUTE),
    DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 22 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 18 MINUTE), 'RIDER_DEMO_COMPLETED'
WHERE @demo_merchant_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders WHERE remark = 'RIDER_DEMO_COMPLETED');

INSERT INTO order_item (order_id, product_id, quantity, price)
SELECT o.id, @demo_product_id, 1, 2600
FROM orders o
WHERE o.remark IN ('RIDER_DEMO_AVAILABLE', 'RIDER_DEMO_ASSIGNED', 'RIDER_DEMO_COMPLETED')
  AND @demo_product_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM order_item item WHERE item.order_id = o.id AND item.product_id = @demo_product_id);

SELECT 'rider_demo_accounts' AS metric, COUNT(*) AS value FROM `user` WHERE phone BETWEEN '13900009001' AND '13900009005'
UNION ALL SELECT 'rider_demo_orders', COUNT(*) FROM orders WHERE remark LIKE 'RIDER_DEMO_%';
