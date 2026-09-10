package com.project.kfpcl_exports.admin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.Statement;
@Component
@RequiredArgsConstructor
public class DbMigrationFix implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(DbMigrationFix.class);

    @Override
    public void run(String... args) {
        // 1. Drop stale FK on product_images
        dropForeignKeyIfExists("product_images", "FK1fjygue3p6b77m88e795sv5r9");

        // 2. Drop stale FK on buyer_rfqs referencing legacy admin_products
        dropForeignKeyIfExists("buyer_rfqs", "FKejjrgyhlanyjddbl69sn8568i");

        // 3. Dynamically find and drop any foreign key on buyer_rfqs referencing admin_products
        dropForeignKeysReferencingTable("buyer_rfqs", "admin_products");

        // 4. Sanitize old non-numeric values in legacy columns if any
        cleanupInvalidColumnData();

        // 5. Ensure AUTO_INCREMENT on id column for tables with IDENTITY generation strategy
        ensureAutoIncrement("products", "id");
        makeColumnNullable("products", "category_id", "BIGINT");
        makeColumnNullable("products", "subcategory_id", "BIGINT");
        makeColumnNullable("products", "store_id", "BIGINT");
        ensureAutoIncrement("admin_categories", "id");
        ensureAutoIncrement("admin_subcategories", "id");
        ensureAutoIncrement("product_images", "id");
        ensureAutoIncrement("banners", "id");
        ensureAutoIncrement("stores", "id");
        ensureAutoIncrement("buyer_rfqs", "id");
        ensureAutoIncrement("rfq_responses", "id");
        ensureAutoIncrement("wishlists", "id");
        ensureAutoIncrement("notifications", "id");
        ensureAutoIncrement("contact_leads", "id");
        ensureAutoIncrement("policies", "id");
        makeColumnNullable("users", "enabled", "BOOLEAN DEFAULT TRUE");
        makeColumnNullable("users", "password", "VARCHAR(255)");
        makeColumnNullable("users", "role", "VARCHAR(50) DEFAULT 'ROLE_BUYER'");
        makeColumnNullable("users", "company_name", "VARCHAR(150) DEFAULT 'KFPCL Buyer'");
        makeColumnNullable("users", "business_type", "VARCHAR(50) DEFAULT 'Buyer'");
        makeColumnNullable("users", "state", "VARCHAR(80) DEFAULT 'India'");
        makeColumnNullable("users", "city", "VARCHAR(80) DEFAULT 'India'");

        // 6. Fix buyer_rfqs.buyer_id to VARCHAR(64) to allow storing UUID string without truncation
        dropForeignKeyOnColumn("buyer_rfqs", "buyer_id");
        modifyColumnType("buyer_rfqs", "buyer_id", "VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        dropForeignKeyOnColumn("rfq_responses", "rfq_id");
        dropForeignKeyOnColumn("notifications", "user_id");
        modifyColumnType("notifications", "user_id", "VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        dropForeignKeysReferencingTable("rfq_responses", "admin_rfqs");
        dropForeignKeysReferencingTable("rfq_responses", "rfqs");
        dropForeignKeysReferencingTable("fcm_tokens", "users");
        dropForeignKeysReferencingTable("addresses", "users");
        dropForeignKeysReferencingTable("notifications", "users");

        // 6b. Fix MySQL collation mismatch between tables (utf8mb4_unicode_ci vs utf8mb4_0900_ai_ci)
        fixCollationMismatches();

        // 7. Backfill any missing buyer_users referenced by legacy buyer_rfqs (e.g. id = 6)
        try {
            jdbcTemplate.execute(
                "INSERT IGNORE INTO buyer_users (id, email, password, name, role, enabled, created_at) " +
                "SELECT DISTINCT r.buyer_id, " +
                "CONCAT('buyer_', r.buyer_id, '@kfpclexports.com'), " +
                "'disabled_password', " +
                "COALESCE(r.buyer_name, CONCAT('Buyer ', r.buyer_id)), " +
                "'ROLE_BUYER', 1, NOW() " +
                "FROM buyer_rfqs r " +
                "LEFT JOIN buyer_users u ON r.buyer_id = u.id " +
                "WHERE u.id IS NULL AND r.buyer_id IS NOT NULL AND r.buyer_id != ''"
            );
        } catch (Exception e) {
            log.debug("Notice on buyer_users backfill: {}", e.getMessage());
        }

        // 8. Backfill missing phone_number in buyer_users extracted from email/name
        try {
            jdbcTemplate.execute(
                "UPDATE buyer_users " +
                "SET phone_number = SUBSTRING(REGEXP_SUBSTR(email, '[0-9]{10}'), -10) " +
                "WHERE (phone_number IS NULL OR phone_number = '') AND email REGEXP '[0-9]{10}'"
            );
            jdbcTemplate.execute(
                "UPDATE buyer_users " +
                "SET phone_number = SUBSTRING(REGEXP_SUBSTR(name, '[0-9]{10}'), -10) " +
                "WHERE (phone_number IS NULL OR phone_number = '') AND name REGEXP '[0-9]{10}'"
            );
        } catch (Exception e) {
            log.debug("Notice on buyer_users phone backfill: {}", e.getMessage());
        }

        // 9. Sync buyer_users into users table so Admin Panel and Authentication have unified data
        try {
            jdbcTemplate.execute(
                "INSERT INTO users (phone_number, full_name, email, company_name, business_type, state, city, is_verified, is_active, enabled, role, created_at, updated_at) " +
                "SELECT " +
                "  b.phone_number, " +
                "  COALESCE(NULLIF(b.name, ''), 'Buyer'), " +
                "  b.email, " +
                "  'KFPCL Buyer', 'Buyer', 'India', 'India', 1, 1, 1, 'ROLE_BUYER', " +
                "  COALESCE(b.created_at, NOW()), NOW() " +
                "FROM buyer_users b " +
                "WHERE b.phone_number IS NOT NULL AND b.phone_number != '' " +
                "  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.phone_number = b.phone_number OR (b.email IS NOT NULL AND b.email != '' AND u.email = b.email))"
            );
        } catch (Exception e) {
            log.debug("Notice on buyer_users sync to users: {}", e.getMessage());
        }
    }

    private void cleanupInvalidColumnData() {
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("UPDATE products SET subcategory_id = NULL WHERE CAST(subcategory_id AS CHAR) REGEXP '[^0-9]'");
            jdbcTemplate.execute("UPDATE products SET category_id = NULL WHERE CAST(category_id AS CHAR) REGEXP '[^0-9]'");
            jdbcTemplate.execute("UPDATE buyer_products SET numeric_price = 150.00, indicative_price = '150.00', original_price = 180.00 WHERE numeric_price IS NULL");
            jdbcTemplate.execute("UPDATE users SET enabled = TRUE WHERE enabled IS NULL");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        } catch (Exception e) {
            log.debug("Data cleanup notice: {}", e.getMessage());
        }
    }

    private void makeColumnNullable(String tableName, String columnName, String columnType) {
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                stmt.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + columnType + " NULL");
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                log.info("Successfully made {}.{} nullable", tableName, columnName);
            } catch (Exception e) {
                log.debug("Could not make {}.{} nullable: {}", tableName, columnName, e.getMessage());
            }
            return null;
        });
    }

    private void ensureAutoIncrement(String tableName, String columnName) {
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                stmt.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " BIGINT NOT NULL AUTO_INCREMENT");
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                log.info("Successfully set AUTO_INCREMENT on {}.{}", tableName, columnName);
            } catch (Exception e) {
                log.debug("Could not set AUTO_INCREMENT on {}.{}: {}", tableName, columnName, e.getMessage());
            }
            return null;
        });
    }

    private void dropForeignKeyIfExists(String tableName, String constraintName) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
            log.info("Successfully dropped foreign key constraint {} from {}.", constraintName, tableName);
        } catch (Exception e) {
            log.debug("Constraint {} on {} might not exist or already dropped: {}", constraintName, tableName, e.getMessage());
        }
    }

    private void dropForeignKeysReferencingTable(String tableName, String referencedTable) {
        try {
            java.util.List<String> fkNames = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND REFERENCED_TABLE_NAME = ?",
                    String.class,
                    tableName,
                    referencedTable
            );
            for (String fk : fkNames) {
                try {
                    jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + fk);
                    log.info("Successfully dropped stale foreign key {} from {} referencing {}", fk, tableName, referencedTable);
                } catch (Exception ex) {
                    log.warn("Could not drop foreign key {} from {}: {}", fk, tableName, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Could not query foreign keys for {} referencing {}: {}", tableName, referencedTable, e.getMessage());
        }
    }

    private void dropForeignKeyOnColumn(String tableName, String columnName) {
        try {
            java.util.List<String> fkNames = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? AND REFERENCED_TABLE_NAME IS NOT NULL",
                    String.class,
                    tableName,
                    columnName
            );
            for (String fk : fkNames) {
                try {
                    jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + fk);
                    log.info("Successfully dropped foreign key {} from {}.{}", fk, tableName, columnName);
                } catch (Exception ex) {
                    log.warn("Could not drop foreign key {} from {}.{}: {}", fk, tableName, columnName, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Could not query foreign keys for {}.{}: {}", tableName, columnName, e.getMessage());
        }
    }

    private void modifyColumnType(String tableName, String columnName, String columnType) {
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                stmt.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + columnType);
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                log.info("Successfully modified {}.{} to {}", tableName, columnName, columnType);
            } catch (Exception e) {
                log.warn("Could not modify {}.{} to {}: {}", tableName, columnName, columnType, e.getMessage());
            }
            return null;
        });
    }

    private void fixCollationMismatches() {
        String[] tables = {
            "buyer_users", "buyer_rfqs", "notifications", "rfq_responses",
            "users", "contact_leads", "wishlists", "products", "buyer_products",
            "admin_products", "categories", "admin_categories", "subcategories", "admin_subcategories"
        };

        for (String table : tables) {
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
                jdbcTemplate.execute("ALTER TABLE " + table + " CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
                log.info("Successfully converted charset and collation for table {}", table);
            } catch (Exception e) {
                log.debug("Notice converting table {}: {}", table, e.getMessage());
            }
        }

        String[] columnSqls = {
            "ALTER TABLE buyer_users MODIFY COLUMN id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL",
            "ALTER TABLE buyer_users MODIFY COLUMN phone_number VARCHAR(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL",
            "ALTER TABLE buyer_users MODIFY COLUMN email VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL",
            "ALTER TABLE buyer_rfqs MODIFY COLUMN buyer_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL",
            "ALTER TABLE buyer_rfqs MODIFY COLUMN buyer_phone VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL",
            "ALTER TABLE notifications MODIFY COLUMN user_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL",
            "ALTER TABLE rfq_responses MODIFY COLUMN rfq_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL"
        };

        for (String sql : columnSqls) {
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
                jdbcTemplate.execute(sql);
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
                log.info("Successfully aligned column collation: {}", sql);
            } catch (Exception e) {
                log.debug("Notice aligning column collation: {} - {}", sql, e.getMessage());
            }
        }
    }
}
