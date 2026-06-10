-- Query performance indexes for dashboard statistics, ranking queries, and batched review/order details.

ALTER TABLE orders
    ADD INDEX idx_orders_create_status (create_time, status),
    ADD INDEX idx_orders_merchant_create_status (merchant_id, create_time, status);

ALTER TABLE order_item
    ADD INDEX idx_order_item_product_order (product_id, order_id);

ALTER TABLE review
    ADD INDEX idx_review_order_id (order_id, id);

ALTER TABLE review_image
    ADD INDEX idx_review_image_review_sort (review_id, sort_order);

ALTER TABLE review_reply
    ADD INDEX idx_review_reply_review_deleted_id (review_id, deleted, id);

ALTER TABLE review_vote
    ADD INDEX idx_review_vote_target (target_type, target_id);

ALTER TABLE review_user_hidden
    ADD INDEX idx_review_hidden_user (user_id, review_id);
