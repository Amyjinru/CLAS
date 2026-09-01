DROP TABLE IF EXISTS deal_redeem_log;
DROP TABLE IF EXISTS user_coupon;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS appeal;
DROP TABLE IF EXISTS user_penalty;
DROP TABLE IF EXISTS deleted_review_backup;
DROP TABLE IF EXISTS review_delete_request;
DROP TABLE IF EXISTS review_user_hidden;
DROP TABLE IF EXISTS review_vote;
DROP TABLE IF EXISTS review_reply;
DROP TABLE IF EXISTS review_image;
DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS service_booking;
DROP TABLE IF EXISTS deal_order;
DROP TABLE IF EXISTS group_deal;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS user_bank_card;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_conversation;
DROP TABLE IF EXISTS delivery_call_session;
DROP TABLE IF EXISTS order_lifecycle_event;
DROP TABLE IF EXISTS order_refund_dispute;
DROP TABLE IF EXISTS rider_daily_metrics;
DROP TABLE IF EXISTS rider_review;
DROP TABLE IF EXISTS rider_tip;
DROP TABLE IF EXISTS rider_withdrawal;
DROP TABLE IF EXISTS rider_settlement;
DROP TABLE IF EXISTS delivery_exception;
DROP TABLE IF EXISTS rider_location_history;
DROP TABLE IF EXISTS rider_audit_log;
DROP TABLE IF EXISTS rider_profile_change_request;
DROP TABLE IF EXISTS rider_profile;
DROP TABLE IF EXISTS rider_application;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS order_lifecycle_event;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS product_category;
DROP TABLE IF EXISTS merchant_audit_log;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS role_application;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS "user";

CREATE TABLE "user" (
    phone VARCHAR(20) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    avatar VARCHAR(512),
    nickname VARCHAR(50),
    session_token VARCHAR(64),
    session_expires_at TIMESTAMP,
    session_device_id VARCHAR(100),
    session_last_seen_at TIMESTAMP,
    pending_login_challenge_id VARCHAR(64),
    pending_login_device_id VARCHAR(100),
    pending_login_created_at TIMESTAMP
);

CREATE TABLE role_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    target_role VARCHAR(20) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_remarks VARCHAR(255),
    operator_id VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_role UNIQUE (user_id, role)
);

CREATE TABLE merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    logo VARCHAR(512),
    phone VARCHAR(20) NOT NULL UNIQUE,
    category VARCHAR(50),
    address VARCHAR(255),
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    delivery_radius_m INT NOT NULL DEFAULT 3000,
    business_hours VARCHAR(100),
    delivery_fee INT NOT NULL DEFAULT 0,
    min_order_price INT NOT NULL DEFAULT 0,
    average_price INT NOT NULL DEFAULT 0,
    score DECIMAL(3,2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    manual_closed BOOLEAN NOT NULL DEFAULT FALSE,
    bank_account VARCHAR(50),
    admin_remarks VARCHAR(255),
    settlement_cycle INT,
    default_prepare_minutes INT NOT NULL DEFAULT 15,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(user_id)
);

CREATE TABLE merchant_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    admin_id VARCHAR(20) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(merchant_id, name)
);

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL
);

CREATE TABLE user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE user_bank_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    bank_name VARCHAR(50) NOT NULL,
    cardholder_name VARCHAR(50) NOT NULL,
    card_no_encrypted VARCHAR(64) NOT NULL,
    card_last4 VARCHAR(4) NOT NULL,
    card_type VARCHAR(20) NOT NULL DEFAULT '借记卡',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL
);

CREATE TABLE favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, merchant_id)
);

CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    type VARCHAR(50),
    target_type VARCHAR(50),
    target_id BIGINT,
    review_id BIGINT,
    reply_id BIGINT,
    order_id BIGINT,
    merchant_id BIGINT,
    target_path VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    rider_id VARCHAR(20),
    total_price INT NOT NULL,
    subtotal INT NOT NULL DEFAULT 0,
    delivery_fee INT NOT NULL DEFAULT 0,
    rider_commission INT NOT NULL DEFAULT 0,
    reassign_count INT NOT NULL DEFAULT 0,
    delivery_sequence INT,
    coupon_discount INT NOT NULL DEFAULT 0,
    user_coupon_id BIGINT,
    status VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(255),
    delivery_contact_name VARCHAR(50),
    delivery_contact_phone VARCHAR(20),
    delivery_longitude DECIMAL(10,6),
    delivery_latitude DECIMAL(10,6),
    distance_meters INT,
    route_distance_meters INT,
    delivery_status VARCHAR(40) NOT NULL DEFAULT 'WAITING',
    rider_accepted_at TIMESTAMP,
    estimated_minutes INT NOT NULL DEFAULT 30,
    prepare_minutes_snapshot INT,
    promise_start_at TIMESTAMP,
    promise_end_at TIMESTAMP,
    predicted_arrival_at TIMESTAMP,
    refund_reason VARCHAR(255),
    refund_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    refund_requested_at TIMESTAMP,
    refund_resolved_at TIMESTAMP,
    remark VARCHAR(255),
    reject_reason VARCHAR(255),
    refund_reject_reason VARCHAR(255),
    create_time TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    accepted_at TIMESTAMP,
    rider_assigned_at TIMESTAMP,
    picked_up_at TIMESTAMP,
    delivery_completed_at TIMESTAMP,
    delivered_at TIMESTAMP,
    completed_at TIMESTAMP,
    canceled_at TIMESTAMP,
    rejected_at TIMESTAMP
);

CREATE TABLE order_refund_dispute (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    rider_id VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    user_reason VARCHAR(500) NOT NULL,
    merchant_reject_reason VARCHAR(255),
    original_order_status VARCHAR(20) NOT NULL,
    original_delivery_status VARCHAR(40),
    admin_reason VARCHAR(500),
    reviewer_id VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP
);

CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price INT NOT NULL
);

CREATE TABLE order_lifecycle_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    from_delivery_status VARCHAR(40),
    to_delivery_status VARCHAR(40),
    actor_role VARCHAR(20) NOT NULL,
    actor_id VARCHAR(30),
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    conversation_type VARCHAR(30) NOT NULL DEFAULT 'USER_MERCHANT',
    merchant_id BIGINT,
    user_id VARCHAR(11) NOT NULL,
    rider_id VARCHAR(20),
    sender_role VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    content CLOB,
    merchant_reply CLOB,
    report_reason VARCHAR(255),
    report_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    created_at TIMESTAMP
);

CREATE TABLE review_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE review_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    parent_reply_id BIGINT,
    user_id VARCHAR(20) NOT NULL,
    reply_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    content CLOB NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE review_vote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    vote_type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(target_type, target_id, user_id)
);

CREATE TABLE review_user_hidden (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(review_id, user_id)
);

CREATE TABLE deleted_review_backup (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    order_id BIGINT NOT NULL,
    score INT NOT NULL,
    content CLOB,
    images_json CLOB,
    deleted_by VARCHAR(20) NOT NULL,
    delete_type VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMP NOT NULL
);

CREATE TABLE review_delete_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    reply_id BIGINT,
    merchant_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',
    reporter_user_id VARCHAR(20),
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_id VARCHAR(20),
    admin_remarks VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    pay_method VARCHAR(20) NOT NULL DEFAULT 'MOCK',
    status VARCHAR(20) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    idempotency_key VARCHAR(128),
    UNIQUE(user_id, idempotency_key)
);

CREATE TABLE announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content CLOB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    create_time TIMESTAMP NOT NULL
);

CREATE TABLE service_booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    appointment_time TIMESTAMP NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE group_deal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    original_price INT NOT NULL,
    deal_price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    valid_days INT NOT NULL DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE deal_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    voucher_code VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    pay_amount INT NOT NULL,
    paid_time TIMESTAMP,
    expire_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL,
    used_time TIMESTAMP
);

CREATE TABLE deal_redeem_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_order_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    voucher_code VARCHAR(40) NOT NULL,
    operator_id VARCHAR(20) NOT NULL,
    redeemed_at TIMESTAMP NOT NULL
);

CREATE TABLE coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    coupon_type VARCHAR(20) NOT NULL DEFAULT 'FIXED',
    discount_amount INT NOT NULL DEFAULT 0,
    discount_percent INT,
    min_order_amount INT NOT NULL DEFAULT 0,
    merchant_id BIGINT,
    total_limit INT NOT NULL DEFAULT 0,
    claimed_count INT NOT NULL DEFAULT 0,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    order_id BIGINT,
    claimed_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    UNIQUE(user_id, coupon_id)
);

CREATE INDEX idx_user_coupon_order_status ON user_coupon (order_id, status);
CREATE INDEX idx_orders_create_status ON orders (create_time, status);
CREATE INDEX idx_orders_merchant_create_status ON orders (merchant_id, create_time, status);
CREATE INDEX idx_order_item_order ON order_item (order_id);
CREATE INDEX idx_order_item_product_order ON order_item (product_id, order_id);
CREATE INDEX idx_review_order_id ON review (order_id, id);
CREATE INDEX idx_review_image_review_sort ON review_image (review_id, sort_order);
CREATE INDEX idx_review_reply_review_deleted_id ON review_reply (review_id, deleted, id);
CREATE INDEX idx_review_vote_target ON review_vote (target_type, target_id);
CREATE INDEX idx_review_hidden_user ON review_user_hidden (user_id, review_id);

ALTER TABLE order_item ADD CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE order_item ADD CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id);
ALTER TABLE payment ADD CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE review ADD CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE user_coupon ADD CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon(id);

CREATE TABLE user_penalty (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    penalty_type VARCHAR(30) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    admin_id VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE appeal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    penalty_id BIGINT,
    content CLOB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_reply CLOB,
    admin_id VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE TABLE rider_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id VARCHAR(20) NOT NULL,
    real_name VARCHAR(50) NOT NULL, id_card_ciphertext VARCHAR(1024) NOT NULL,
    id_card_masked VARCHAR(32) NOT NULL, vehicle_type VARCHAR(20) NOT NULL,
    service_area VARCHAR(100) NOT NULL, emergency_contact_name VARCHAR(50) NOT NULL,
    emergency_contact_phone VARCHAR(20) NOT NULL, credential_urls CLOB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', reject_reason VARCHAR(255),
    reviewer_id VARCHAR(20), reviewed_at TIMESTAMP, created_at TIMESTAMP NOT NULL
);

CREATE TABLE rider_profile (
    user_id VARCHAR(20) PRIMARY KEY, real_name VARCHAR(50) NOT NULL,
    id_card_ciphertext VARCHAR(1024) NOT NULL, id_card_masked VARCHAR(32) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL, service_area VARCHAR(100) NOT NULL,
    emergency_contact_name VARCHAR(50) NOT NULL, emergency_contact_phone VARCHAR(20) NOT NULL, service_phone VARCHAR(20),
    online_status BOOLEAN NOT NULL DEFAULT FALSE, accepting_orders BOOLEAN NOT NULL DEFAULT FALSE, max_active_orders INT NOT NULL DEFAULT 3,
    current_longitude DECIMAL(10,6), current_latitude DECIMAL(10,6), location_updated_at TIMESTAMP,
    withdrawable_balance INT NOT NULL DEFAULT 0, frozen_balance INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED', created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL
);

CREATE TABLE rider_audit_log (id BIGINT PRIMARY KEY AUTO_INCREMENT, rider_id VARCHAR(20) NOT NULL, operator_id VARCHAR(20) NOT NULL, action VARCHAR(50) NOT NULL, reason VARCHAR(255), before_value CLOB, after_value CLOB, created_at TIMESTAMP NOT NULL);
CREATE TABLE rider_profile_change_request (id BIGINT PRIMARY KEY AUTO_INCREMENT, rider_id VARCHAR(20) NOT NULL, current_phone VARCHAR(20) NOT NULL, requested_phone VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', review_reason VARCHAR(255), reviewer_id VARCHAR(20), reviewed_at TIMESTAMP, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL);
CREATE TABLE rider_location_history (id BIGINT PRIMARY KEY AUTO_INCREMENT, rider_id VARCHAR(20) NOT NULL, longitude DECIMAL(10,6) NOT NULL, latitude DECIMAL(10,6) NOT NULL, accuracy_meters INT, reported_at TIMESTAMP NOT NULL);
CREATE TABLE delivery_exception (id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, rider_id VARCHAR(20), exception_type VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'OPEN', score_deduction INT NOT NULL DEFAULT 0, commission_deduction INT NOT NULL DEFAULT 0, detail VARCHAR(255), created_at TIMESTAMP NOT NULL, UNIQUE(order_id, exception_type));
CREATE TABLE rider_settlement (id BIGINT PRIMARY KEY AUTO_INCREMENT, rider_id VARCHAR(20) NOT NULL, order_id BIGINT, source_type VARCHAR(30) NOT NULL, source_id VARCHAR(64) NOT NULL, settlement_type VARCHAR(30) NOT NULL, amount INT NOT NULL, balance_type VARCHAR(20) NOT NULL, created_at TIMESTAMP NOT NULL, UNIQUE(source_type, source_id));
CREATE TABLE rider_withdrawal (id BIGINT PRIMARY KEY AUTO_INCREMENT, rider_id VARCHAR(20) NOT NULL, bank_card_id BIGINT NOT NULL, amount INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', reviewer_id VARCHAR(20), review_reason VARCHAR(255), created_at TIMESTAMP NOT NULL, reviewed_at TIMESTAMP);
CREATE TABLE rider_tip (id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL UNIQUE, user_id VARCHAR(20) NOT NULL, rider_id VARCHAR(20) NOT NULL, amount INT NOT NULL, idempotency_key VARCHAR(100) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PAID', paid_at TIMESTAMP NOT NULL, UNIQUE(user_id, idempotency_key));
CREATE TABLE rider_review (id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL UNIQUE, user_id VARCHAR(20) NOT NULL, rider_id VARCHAR(20) NOT NULL, score INT NOT NULL, tags VARCHAR(255), content VARCHAR(500), created_at TIMESTAMP NOT NULL);
CREATE TABLE rider_daily_metrics (id BIGINT PRIMARY KEY AUTO_INCREMENT, rider_id VARCHAR(20) NOT NULL, metric_date DATE NOT NULL, completed_orders INT NOT NULL DEFAULT 0, net_income INT NOT NULL DEFAULT 0, average_rating DECIMAL(3,2), overdue_count INT NOT NULL DEFAULT 0, average_delivery_minutes INT NOT NULL DEFAULT 0, base_score DECIMAL(5,2) NOT NULL DEFAULT 0, manual_adjustment DECIMAL(5,2) NOT NULL DEFAULT 0, final_score DECIMAL(5,2) NOT NULL DEFAULT 0, grade VARCHAR(2) NOT NULL DEFAULT 'D', archived_at TIMESTAMP, UNIQUE(rider_id, metric_date));
CREATE TABLE delivery_call_session (id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, rider_id VARCHAR(20) NOT NULL, user_id VARCHAR(20) NOT NULL, masked_phone VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL);
CREATE TABLE chat_conversation (id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, conversation_type VARCHAR(30) NOT NULL, user_id VARCHAR(20) NOT NULL, peer_id VARCHAR(20) NOT NULL, last_message_at TIMESTAMP, created_at TIMESTAMP NOT NULL, UNIQUE(order_id, conversation_type));

INSERT INTO "user" (phone, username, password, role, enabled, avatar, nickname) VALUES
    ('13800000001', 'user', 'Abc123!', 'USER', TRUE, NULL, 'User One'),
    ('13800000002', 'merchant', 'Abc123!', 'MERCHANT', TRUE, NULL, 'Merchant One'),
    ('13800000003', 'admin', 'Abc123!', 'ADMIN', TRUE, NULL, 'Admin One'),
    ('13800000004', 'rider_one', 'Abc123!', 'USER', TRUE, NULL, 'Rider One'),
    ('13800000005', 'rider_two', 'Abc123!', 'USER', TRUE, NULL, 'Rider Two'),
    ('13800000008', 'rider_eight', 'Abc123!', 'USER', TRUE, NULL, 'Rider Eight'),
    ('13345678903', 'delivery_demo_rider_a', 'Abc123!', 'USER', TRUE, NULL, 'Delivery Demo Rider'),
    ('14000000001', 'merchant_fourteen', 'Abc123!', 'MERCHANT', TRUE, NULL, 'Merchant Fourteen'),
    ('13345678901', 'delivery_demo_merchant', 'Abc123!', 'MERCHANT', TRUE, NULL, 'Delivery Demo Merchant');

INSERT INTO user_role (user_id, role, status, created_at, updated_at) VALUES
    ('13800000001', 'USER', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000002', 'MERCHANT', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000003', 'ADMIN', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000004', 'USER', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000004', 'RIDER', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000005', 'USER', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000005', 'RIDER', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000008', 'USER', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000008', 'RIDER', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13345678903', 'USER', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13345678903', 'RIDER', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('14000000001', 'MERCHANT', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13345678901', 'MERCHANT', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rider_profile (user_id, real_name, id_card_ciphertext, id_card_masked, vehicle_type, service_area, emergency_contact_name, emergency_contact_phone, status, created_at, updated_at) VALUES
    ('13800000004', 'Test Rider One', 'test-ciphertext-rider-one', '110***********0001', 'E_BIKE', 'Campus', 'Emergency One', '13800000004', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000005', 'Test Rider Two', 'test-ciphertext-rider-two', '110***********0002', 'E_BIKE', 'Campus', 'Emergency Two', '13800000005', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13800000008', 'Test Rider Eight', 'test-ciphertext-rider-eight', '110***********0008', 'E_BIKE', 'Campus', 'Emergency Eight', '13800000008', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13345678903', 'Delivery Demo Rider', 'test-ciphertext-delivery-demo', '110***********0003', 'E_BIKE', 'Campus', 'Emergency Demo', '13345678903', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO merchant (
    id, user_id, merchant_name, phone, category, address, longitude, latitude,
    delivery_radius_m, business_hours, delivery_fee, min_order_price,
    average_price, score, status, bank_account, admin_remarks, settlement_cycle,
    created_at, updated_at
) VALUES
(
    1, '13800000002', 'Campus Light Meals', '13800000022', 'Food', 'Software Park East Gate No.1',
    116.397428, 39.909230, 3000, '00:00-23:59', 300, 1500, 2800, 4.70, 'OPEN',
    '6222000000000000001', 'Seed merchant for integration tests', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    2, '14000000001', 'Merchant Fourteen', '14000000001', 'Food', 'Campus Demo Zone',
    116.397428, 39.909230, 3000, '09:00-22:00', 300, 1200, 2200, 4.60, 'OPEN',
    '6222000000000000002', 'Quick-login merchant seed', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    3, '13345678901', 'Delivery Demo Restaurant', '13345678901', 'Food', 'Delivery Demo Zone',
    116.397428, 39.909230, 5000, '09:00-22:00', 300, 0, 2600, 4.80, 'OPEN',
    '6222000000000000003', 'Delivery demo merchant seed', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO product_category (id, merchant_id, name, sort_order, created_at, updated_at) VALUES
    (1, 1, 'Main', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product (id, merchant_id, category_id, name, description, price, stock, image, status, created_at, updated_at) VALUES
    (1, 1, 1, 'Chicken Energy Bowl', 'Low calorie test product', 2590, 30, '/images/product-1.jpg', 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_address (id, user_id, contact_name, phone, address, longitude, latitude, is_default, created_at, updated_at) VALUES
    (1, '13800000001', 'Test User', '13800000001', 'Software College A-302', 116.398000, 39.910000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO announcement (id, title, content, status, create_time) VALUES
    (1, 'Test Announcement', 'Integration test announcement', 'PUBLISHED', CURRENT_TIMESTAMP);

INSERT INTO service_booking (id, user_id, merchant_id, service_name, appointment_time, contact_phone, note, status, created_at, updated_at) VALUES
    (1, '13800000001', 1, 'In-store consultation', DATEADD('DAY', 1, CURRENT_TIMESTAMP), '13800000001', 'Seed booking', 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO group_deal (id, merchant_id, title, description, original_price, deal_price, stock, valid_days, status, created_at, updated_at) VALUES
    (1, 1, 'Two-person light meal set', 'Integration test group deal', 6400, 4990, 50, 30, 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO coupon (id, title, description, coupon_type, discount_amount, min_order_amount, merchant_id, total_limit, claimed_count, valid_from, valid_to, status, created_at) VALUES
    (1, 'New user discount', 'Integration test coupon', 'FIXED', 300, 2000, NULL, 1000, 0, CURRENT_TIMESTAMP, DATEADD('DAY', 90, CURRENT_TIMESTAMP), 'ACTIVE', CURRENT_TIMESTAMP);

ALTER TABLE merchant ALTER COLUMN id RESTART WITH 10;
ALTER TABLE merchant_audit_log ALTER COLUMN id RESTART WITH 10;
ALTER TABLE product_category ALTER COLUMN id RESTART WITH 10;
ALTER TABLE product ALTER COLUMN id RESTART WITH 10;
ALTER TABLE cart ALTER COLUMN id RESTART WITH 10;
ALTER TABLE user_address ALTER COLUMN id RESTART WITH 10;
ALTER TABLE favorite ALTER COLUMN id RESTART WITH 10;
ALTER TABLE notification ALTER COLUMN id RESTART WITH 10;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 100;
ALTER TABLE order_item ALTER COLUMN id RESTART WITH 10;
ALTER TABLE review ALTER COLUMN id RESTART WITH 10;
ALTER TABLE review_image ALTER COLUMN id RESTART WITH 10;
ALTER TABLE review_reply ALTER COLUMN id RESTART WITH 10;
ALTER TABLE review_vote ALTER COLUMN id RESTART WITH 10;
ALTER TABLE review_user_hidden ALTER COLUMN id RESTART WITH 10;
ALTER TABLE review_delete_request ALTER COLUMN id RESTART WITH 10;
ALTER TABLE deleted_review_backup ALTER COLUMN id RESTART WITH 10;
ALTER TABLE payment ALTER COLUMN id RESTART WITH 10;
ALTER TABLE announcement ALTER COLUMN id RESTART WITH 10;
ALTER TABLE service_booking ALTER COLUMN id RESTART WITH 10;
ALTER TABLE group_deal ALTER COLUMN id RESTART WITH 10;
ALTER TABLE deal_order ALTER COLUMN id RESTART WITH 10;
ALTER TABLE deal_redeem_log ALTER COLUMN id RESTART WITH 10;
ALTER TABLE coupon ALTER COLUMN id RESTART WITH 10;
ALTER TABLE user_coupon ALTER COLUMN id RESTART WITH 10;
ALTER TABLE user_penalty ALTER COLUMN id RESTART WITH 10;
ALTER TABLE appeal ALTER COLUMN id RESTART WITH 10;
