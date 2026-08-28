package com.clas.config;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseCompatibilityConfig {
    private static final Logger log = LoggerFactory.getLogger(DatabaseCompatibilityConfig.class);

    @Bean
    ApplicationRunner ensureMerchantCompatibilityColumns(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return args -> {
            if (!hasColumn(dataSource, "merchant", "logo")) {
                log.info("Adding missing merchant.logo column for store logo uploads");
                jdbcTemplate.execute("ALTER TABLE merchant ADD COLUMN logo VARCHAR(512)");
            }
            if (!hasColumn(dataSource, "merchant", "manual_closed")) {
                log.info("Adding missing merchant.manual_closed column for manual closing");
                jdbcTemplate.execute("ALTER TABLE merchant ADD COLUMN manual_closed BOOLEAN NOT NULL DEFAULT FALSE");
            }
            // orders 生命周期时间戳 — 出厂 schema 可能缺少
            addOrderColumnIfMissing(dataSource, jdbcTemplate, "paid_at", "DATETIME NULL", "orders.paid_at");
            addOrderColumnIfMissing(dataSource, jdbcTemplate, "accepted_at", "DATETIME NULL", "orders.accepted_at");
            addOrderColumnIfMissing(dataSource, jdbcTemplate, "delivered_at", "DATETIME NULL", "orders.delivered_at");
            addOrderColumnIfMissing(dataSource, jdbcTemplate, "completed_at", "DATETIME NULL", "orders.completed_at");
            addOrderColumnIfMissing(dataSource, jdbcTemplate, "canceled_at", "DATETIME NULL", "orders.canceled_at");
            addOrderColumnIfMissing(dataSource, jdbcTemplate, "rejected_at", "DATETIME NULL", "orders.rejected_at");
            // notification 扩展字段
            addColumnIfMissing(dataSource, jdbcTemplate, "notification", "type", "VARCHAR(50) DEFAULT 'SYSTEM'", "notification.type");
            addColumnIfMissing(dataSource, jdbcTemplate, "notification", "target_type", "VARCHAR(50) NULL", "notification.target_type");
            addColumnIfMissing(dataSource, jdbcTemplate, "notification", "target_id", "BIGINT NULL", "notification.target_id");
            addColumnIfMissing(dataSource, jdbcTemplate, "notification", "review_id", "BIGINT NULL", "notification.review_id");
            addColumnIfMissing(dataSource, jdbcTemplate, "notification", "reply_id", "BIGINT NULL", "notification.reply_id");
            addColumnIfMissing(dataSource, jdbcTemplate, "notification", "order_id", "BIGINT NULL", "notification.order_id");
            addColumnIfMissing(dataSource, jdbcTemplate, "notification", "merchant_id", "BIGINT NULL", "notification.merchant_id");
            addColumnIfMissing(dataSource, jdbcTemplate, "notification", "target_path", "VARCHAR(255) NULL", "notification.target_path");
            // payment 幂等键
            addColumnIfMissing(dataSource, jdbcTemplate, "payment", "idempotency_key", "VARCHAR(64) NULL", "payment.idempotency_key");
        };
    }

    private void addColumnIfMissing(DataSource dataSource, JdbcTemplate jdbcTemplate,
                                    String table, String column, String type, String label) throws Exception {
        if (!hasColumn(dataSource, table, column)) {
            log.info("Adding missing {} column", label);
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }

    private void addOrderColumnIfMissing(DataSource dataSource, JdbcTemplate jdbcTemplate,
                                         String column, String type, String label) throws Exception {
        addColumnIfMissing(dataSource, jdbcTemplate, "orders", column, type, label);
    }

    private boolean hasColumn(DataSource dataSource, String tableName, String columnName) throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String[] tableCandidates = { tableName, tableName.toUpperCase(), tableName.toLowerCase() };
            String[] columnCandidates = { columnName, columnName.toUpperCase(), columnName.toLowerCase() };
            for (String table : tableCandidates) {
                for (String column : columnCandidates) {
                    try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, table, column)) {
                        if (columns.next()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
