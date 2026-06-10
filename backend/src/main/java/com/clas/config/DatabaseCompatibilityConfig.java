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
        };
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
