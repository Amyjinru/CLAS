package com.clas.catalog.service;

import com.clas.catalog.api.CatalogCategory;
import com.clas.catalog.api.CatalogItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
    private static final String PRODUCT_COLUMNS = """
            p.id, p.merchant_id, p.category_id, c.name AS category_name,
            p.name, p.description, p.price, p.stock, p.status, p.image
            """;

    private final JdbcTemplate jdbcTemplate;

    public CatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CatalogItem> listOnSaleByMerchant(Long merchantId) {
        return jdbcTemplate.query("""
                SELECT %s
                FROM product p
                LEFT JOIN product_category c ON c.id = p.category_id AND c.merchant_id = p.merchant_id
                WHERE p.merchant_id = ? AND p.status = 'ON_SALE'
                ORDER BY p.category_id, p.id
                """.formatted(PRODUCT_COLUMNS), productMapper(), merchantId);
    }

    public List<CatalogCategory> listCategories(Long merchantId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, name, sort_order
                FROM product_category
                WHERE merchant_id = ?
                ORDER BY sort_order, id
                """, (resultSet, rowNum) -> new CatalogCategory(
                resultSet.getLong("id"), resultSet.getLong("merchant_id"), resultSet.getString("name"),
                resultSet.getInt("sort_order")), merchantId);
    }

    public Optional<CatalogItem> findOnSaleById(Long productId, Long merchantId) {
        List<CatalogItem> result = jdbcTemplate.query("""
                SELECT %s
                FROM product p
                LEFT JOIN product_category c ON c.id = p.category_id AND c.merchant_id = p.merchant_id
                WHERE p.id = ? AND p.merchant_id = ? AND p.status = 'ON_SALE'
                """.formatted(PRODUCT_COLUMNS), productMapper(), productId, merchantId);
        return result.stream().findFirst();
    }

    private RowMapper<CatalogItem> productMapper() {
        return this::mapProduct;
    }

    private CatalogItem mapProduct(ResultSet resultSet, int rowNum) throws SQLException {
        return new CatalogItem(
                resultSet.getLong("id"),
                resultSet.getLong("merchant_id"),
                resultSet.getObject("category_id", Long.class),
                resultSet.getString("category_name"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("price"),
                resultSet.getInt("stock"),
                resultSet.getString("status"),
                resultSet.getString("image")
        );
    }
}
