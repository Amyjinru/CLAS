-- One-time cutover script. Run after database/catalog-service-schema.sql and before
-- routing public product reads to catalog-service. Re-running is safe for unchanged rows.
INSERT INTO catalog_db.product_category (id, merchant_id, name, sort_order, created_at, updated_at)
SELECT id, merchant_id, name, sort_order, created_at, updated_at
FROM clas.product_category
ON DUPLICATE KEY UPDATE
    merchant_id = VALUES(merchant_id),
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    updated_at = VALUES(updated_at);

INSERT INTO catalog_db.product (
    id, merchant_id, category_id, name, description, price, stock, status, image, created_at, updated_at
)
SELECT id, merchant_id, category_id, name, description, price, stock, status, image, created_at, updated_at
FROM clas.product
ON DUPLICATE KEY UPDATE
    merchant_id = VALUES(merchant_id),
    category_id = VALUES(category_id),
    name = VALUES(name),
    description = VALUES(description),
    price = VALUES(price),
    stock = VALUES(stock),
    status = VALUES(status),
    image = VALUES(image),
    updated_at = VALUES(updated_at);
