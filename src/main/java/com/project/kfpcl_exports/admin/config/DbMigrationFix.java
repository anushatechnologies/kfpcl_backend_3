package com.project.kfpcl_exports.admin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
