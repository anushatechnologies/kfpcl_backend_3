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
        try {
            jdbcTemplate.execute("ALTER TABLE product_images DROP FOREIGN KEY FK1fjygue3p6b77m88e795sv5r9");
            log.info("Successfully dropped old foreign key constraint FK1fjygue3p6b77m88e795sv5r9 from product_images.");
        } catch (Exception e) {
            log.debug("Foreign key constraint FK1fjygue3p6b77m88e795sv5r9 might already be dropped or not exist: {}", e.getMessage());
        }
    }
}
