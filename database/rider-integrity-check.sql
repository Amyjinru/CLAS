-- Run after migration-20260825-rider-delivery.sql. Every query must return zero rows.
USE clas;

SELECT order_id, COUNT(*) AS duplicate_tips FROM rider_tip GROUP BY order_id HAVING COUNT(*) > 1;
SELECT order_id, COUNT(*) AS duplicate_reviews FROM rider_review GROUP BY order_id HAVING COUNT(*) > 1;
SELECT user_id, COUNT(*) AS duplicate_profiles FROM rider_profile GROUP BY user_id HAVING COUNT(*) > 1;
SELECT source_type, source_id, COUNT(*) AS duplicate_settlements
FROM rider_settlement GROUP BY source_type, source_id HAVING COUNT(*) > 1;
SELECT u.phone, u.role FROM `user` u
LEFT JOIN user_role ur ON ur.user_id = u.phone AND ur.role = u.role
WHERE ur.id IS NULL;
