-- 将种子产品状态统一设为 ON_SALE，使统计数据准确
USE clas;
UPDATE product SET status = 'ON_SALE' WHERE status = 'OFF_SALE' AND id > 0;
