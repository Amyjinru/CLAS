-- Core relationship constraints.
-- Review database/orphan-detection-core.sql before running this migration.
-- The cleanup statements below remove rows that cannot be interpreted without their parent.

DELETE oi FROM order_item oi LEFT JOIN orders o ON o.id = oi.order_id WHERE o.id IS NULL;
DELETE oi FROM order_item oi LEFT JOIN product p ON p.id = oi.product_id WHERE p.id IS NULL;
DELETE p FROM payment p LEFT JOIN orders o ON o.id = p.order_id WHERE o.id IS NULL;
DELETE r FROM review r LEFT JOIN orders o ON o.id = r.order_id WHERE o.id IS NULL;
DELETE uc FROM user_coupon uc LEFT JOIN coupon c ON c.id = uc.coupon_id WHERE c.id IS NULL;

ALTER TABLE order_item
    ADD CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
    ADD CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id);

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE review
    ADD CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE user_coupon
    ADD CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon(id);
