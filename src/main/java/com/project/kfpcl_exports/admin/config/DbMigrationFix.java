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
        ensureAutoIncrement("admin_rfqs", "id");
        ensureAutoIncrement("quotations", "id");
        ensureAutoIncrement("rfq_responses", "id");
        ensureAutoIncrement("wishlists", "id");
        ensureAutoIncrement("notifications", "id");
        ensureAutoIncrement("contact_leads", "id");
        ensureAutoIncrement("policies", "id");
        makeColumnNullable("admin_rfqs", "details", "VARCHAR(2000)");
        makeColumnNullable("admin_rfqs", "destination_country", "VARCHAR(255)");
        makeColumnNullable("admin_rfqs", "shipping_terms", "VARCHAR(255)");
        makeColumnNullable("admin_rfqs", "target_price", "VARCHAR(255)");
        makeColumnNullable("admin_rfqs", "unit", "VARCHAR(50)");
        makeColumnNullable("admin_rfqs", "quantity", "INT");
        makeColumnNullable("admin_rfqs", "customer_id", "BIGINT");
        makeColumnNullable("admin_rfqs", "rfq_number", "VARCHAR(255)");
        makeColumnNullable("admin_rfqs", "customer_name", "VARCHAR(255)");
        makeColumnNullable("admin_rfqs", "customer_email", "VARCHAR(255)");
        makeColumnNullable("admin_rfqs", "customer_phone", "VARCHAR(255)");
        makeColumnNullable("admin_rfqs", "product_name", "VARCHAR(255)");
        makeColumnNullable("admin_rfqs", "status", "VARCHAR(50)");
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

        // 10. Sync existing buyer_rfqs into admin_rfqs table so all past RFQs are in admin_rfqs
        try {
            jdbcTemplate.execute(
                "INSERT INTO admin_rfqs (rfq_number, customer_name, customer_email, customer_phone, product_name, quantity, unit, destination_country, shipping_terms, details, status, created_at) " +
                "SELECT " +
                "  r.rfq_code, " +
                "  COALESCE(NULLIF(r.buyer_name, ''), NULLIF(u.name, ''), 'Buyer'), " +
                "  u.email, " +
                "  COALESCE(NULLIF(r.buyer_phone, ''), u.phone_number), " +
                "  COALESCE(NULLIF(p.name, ''), 'Commodity Product'), " +
                "  CAST(COALESCE(NULLIF(REGEXP_SUBSTR(r.quantity, '[0-9]+'), ''), '1') AS UNSIGNED), " +
                "  COALESCE(NULLIF(TRIM(REGEXP_REPLACE(r.quantity, '^[0-9, ]+', '')), ''), 'Units'), " +
                "  COALESCE(r.delivery_location, 'India'), " +
                "  'FOB / Standard', " +
                "  r.buyer_message, " +
                "  COALESCE(r.status, 'PENDING'), " +
                "  COALESCE(r.created_at, NOW()) " +
                "FROM buyer_rfqs r " +
                "LEFT JOIN buyer_users u ON r.buyer_id = u.id " +
                "LEFT JOIN buyer_products p ON r.product_id = p.id " +
                "WHERE r.rfq_code IS NOT NULL " +
                "  AND NOT EXISTS (SELECT 1 FROM admin_rfqs a WHERE a.rfq_number = r.rfq_code)"
            );
            log.info("Successfully synced existing buyer_rfqs into admin_rfqs table");
        } catch (Exception e) {
            log.debug("Notice on buyer_rfqs sync to admin_rfqs: {}", e.getMessage());
        }

        // 11. Clean up previously concatenated "Buyer Name: ... Buyer Mobile: ..." from buyer_message in buyer_rfqs
        try {
            java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, buyer_name, buyer_phone, subject, buyer_message FROM buyer_rfqs WHERE buyer_message LIKE '%Buyer Name:%'"
            );
            for (java.util.Map<String, Object> row : rows) {
                Long id = ((Number) row.get("id")).longValue();
                String rawMsg = (String) row.get("buyer_message");
                String bName = (String) row.get("buyer_name");
                String bPhone = (String) row.get("buyer_phone");
                String subj = (String) row.get("subject");

                if (rawMsg != null) {
                    String clean = rawMsg;
                    java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile("(?i)Buyer\\s*Name\\s*:\\s*([^\\n]+?)(?=(?:Buyer\\s*Mobile|Mobile|Phone|Subject|Date|$))").matcher(clean);
                    if (nameMatcher.find()) {
                        if (bName == null || bName.isBlank()) bName = nameMatcher.group(1).trim();
                        clean = clean.replace(nameMatcher.group(0), "").trim();
                    }

                    java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern.compile("(?i)(?:Buyer\\s*Mobile|Mobile|Phone)\\s*:\\s*([^\\n]+?)(?=(?:Subject|Date|Buyer\\s*Name|$))").matcher(clean);
                    if (phoneMatcher.find()) {
                        if (bPhone == null || bPhone.isBlank()) bPhone = phoneMatcher.group(1).trim();
                        clean = clean.replace(phoneMatcher.group(0), "").trim();
                    }

                    java.util.regex.Matcher subjectMatcher = java.util.regex.Pattern.compile("(?i)Subject\\s*:\\s*([^\\n]+?)(?=(?:Message|Notes|Requirement|Date|$))").matcher(clean);
                    if (subjectMatcher.find()) {
                        if (subj == null || subj.isBlank()) subj = subjectMatcher.group(1).trim();
                        clean = clean.replace(subjectMatcher.group(0), "").trim();
                    }

                    clean = clean.replaceAll("(?i)^(?:Message|Notes|Requirement|Details)\\s*:\\s*", "").trim();
                    if (clean.isBlank()) {
                        clean = (subj != null ? subj : "Price enquiry");
                    }

                    jdbcTemplate.update(
                        "UPDATE buyer_rfqs SET buyer_name = ?, buyer_phone = ?, subject = ?, buyer_message = ? WHERE id = ?",
                        bName, bPhone, subj, clean, id
                    );
                }
            }
        } catch (Exception e) {
            log.debug("Notice on buyer_message cleaning: {}", e.getMessage());
        }

        // 12. Clean quantity field: extract only numeric + unit, remove embedded product names
        //     e.g. "10 Aashirvaad Shudh Chakki Atta (Standard pack)" → "10"
        //     e.g. "500 Kg" → "500 Kg" (no change needed)
        try {
            java.util.List<java.util.Map<String, Object>> qtyRows = jdbcTemplate.queryForList(
                "SELECT id, quantity FROM buyer_rfqs WHERE quantity REGEXP '^[0-9][0-9,.]* [A-Za-z]' AND quantity NOT REGEXP '^[0-9][0-9,.]* ?(kg|g|mt|ton|tons|metric tons|pieces|pcs|units|litres|liters|l|box|boxes|cartons|KG|MT|L)$'"
            );
            for (java.util.Map<String, Object> row : qtyRows) {
                Long id = ((Number) row.get("id")).longValue();
                String rawQty = (String) row.get("quantity");
                if (rawQty != null) {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("^([0-9][0-9,.]*)\\s*(kg|g|mt|ton|tons|metric tons|pieces|pcs|units|litres|liters|l|box|boxes|cartons|KG|MT|L)?", java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(rawQty.trim());
                    if (m.find()) {
                        String num = m.group(1).trim();
                        String unit = m.group(2) != null ? m.group(2).trim() : "";
                        String cleanQty = unit.isEmpty() ? num : (num + " " + unit);
                        jdbcTemplate.update("UPDATE buyer_rfqs SET quantity = ? WHERE id = ?", cleanQty, id);
                    }
                }
            }
            log.info("Cleaned quantity field in buyer_rfqs");
        } catch (Exception e) {
            log.debug("Notice on quantity cleaning: {}", e.getMessage());
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
