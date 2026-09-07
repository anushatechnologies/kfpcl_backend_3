package com.project.kfpcl_exports.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Runs raw SQL fixes directly on the DataSource BEFORE Hibernate initializes its DDL auto-update schema migration.
 */
@Configuration
public class PreHibernateFix {

    private static final Logger log = LoggerFactory.getLogger(PreHibernateFix.class);

    public PreHibernateFix(DataSource dataSource) {
        log.info("Running pre-Hibernate database schema cleanup and auto-increment fixes...");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Disable foreign key checks for table modifications
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // 1. Sanitize legacy string values in subcategory_id / category_id before Hibernate tries to alter column to BIGINT
            executeQuietly(stmt, "UPDATE products SET subcategory_id = NULL WHERE CAST(subcategory_id AS CHAR) REGEXP '[^0-9]'");
            executeQuietly(stmt, "UPDATE products SET category_id = NULL WHERE CAST(category_id AS CHAR) REGEXP '[^0-9]'");
            executeQuietly(stmt, "UPDATE notifications SET id = '0' WHERE CAST(id AS CHAR) REGEXP '[^0-9]'");

            // 2. Ensure AUTO_INCREMENT on id column for products and related tables, and make category_id/subcategory_id nullable
            executeQuietly(stmt, "ALTER TABLE products MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
            executeQuietly(stmt, "ALTER TABLE products MODIFY COLUMN category_id BIGINT NULL");
            executeQuietly(stmt, "ALTER TABLE products MODIFY COLUMN subcategory_id BIGINT NULL");
            executeQuietly(stmt, "ALTER TABLE admin_categories MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
            executeQuietly(stmt, "ALTER TABLE admin_subcategories MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
            executeQuietly(stmt, "ALTER TABLE product_images MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
            executeQuietly(stmt, "ALTER TABLE banners MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
            executeQuietly(stmt, "ALTER TABLE stores MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
            executeQuietly(stmt, "ALTER TABLE buyer_rfqs MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
            executeQuietly(stmt, "ALTER TABLE rfq_responses MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");

            // Re-enable foreign key checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            log.info("Pre-Hibernate database schema cleanup completed successfully.");

        } catch (Exception e) {
            log.warn("Pre-Hibernate schema fix encountered an issue (safe to ignore if first run): {}", e.getMessage());
        }
    }

    private void executeQuietly(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (Exception e) {
            log.debug("Quiet execution note for query [{}] : {}", sql, e.getMessage());
        }
    }
}
