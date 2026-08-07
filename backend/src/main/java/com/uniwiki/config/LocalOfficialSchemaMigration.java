package com.uniwiki.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class LocalOfficialSchemaMigration implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> constraints = new ArrayList<>();
        List<String> foreignKeys = new ArrayList<>();
        List<String> indexes = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT tc.constraint_name
                     FROM information_schema.table_constraints tc
                     JOIN information_schema.constraint_column_usage ccu
                       ON tc.constraint_catalog = ccu.constraint_catalog
                      AND tc.constraint_schema = ccu.constraint_schema
                      AND tc.constraint_name = ccu.constraint_name
                     WHERE LOWER(tc.table_name) = 'official_wiki_documents'
                       AND tc.constraint_type = 'UNIQUE'
                     GROUP BY tc.constraint_name
                     HAVING COUNT(*) = 1
                        AND MAX(LOWER(ccu.column_name)) = 'wiki_post_id'
                     """)) {
            while (result.next()) {
                constraints.add(result.getString(1));
            }
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String constraint : constraints) {
                if (!constraint.matches("[A-Za-z0-9_]+")) {
                    throw new IllegalStateException("Unexpected H2 constraint name: " + constraint);
                }
                statement.execute("ALTER TABLE official_wiki_documents DROP CONSTRAINT \"" + constraint + "\"");
                log.info("Removed legacy local official wiki constraint: {}", constraint);
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT DISTINCT tc.constraint_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.constraint_column_usage ccu
                      ON tc.constraint_catalog = ccu.constraint_catalog
                     AND tc.constraint_schema = ccu.constraint_schema
                     AND tc.constraint_name = ccu.constraint_name
                    WHERE LOWER(tc.table_name) = 'official_wiki_documents'
                      AND tc.constraint_type = 'FOREIGN KEY'
                      AND LOWER(ccu.column_name) = 'wiki_post_id'
                    """)) {
                while (result.next()) {
                    foreignKeys.add(result.getString(1));
                }
            }
            for (String foreignKey : foreignKeys) {
                if (!foreignKey.matches("[A-Za-z0-9_]+")) {
                    throw new IllegalStateException("Unexpected H2 foreign key name: " + foreignKey);
                }
                statement.execute("ALTER TABLE official_wiki_documents DROP CONSTRAINT \"" + foreignKey + "\"");
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT DISTINCT index_name
                    FROM information_schema.index_columns
                    WHERE LOWER(table_name) = 'official_wiki_documents'
                      AND LOWER(column_name) = 'wiki_post_id'
                    """)) {
                while (result.next()) {
                    indexes.add(result.getString(1));
                }
            }
            for (String index : indexes) {
                if (!index.matches("[A-Za-z0-9_]+")) {
                    throw new IllegalStateException("Unexpected H2 index name: " + index);
                }
                statement.execute("DROP INDEX IF EXISTS \"" + index + "\"");
                log.info("Removed legacy local official wiki index: {}", index);
            }
            if (!foreignKeys.isEmpty()) {
                statement.execute("""
                        ALTER TABLE official_wiki_documents
                        ADD CONSTRAINT fk_official_wiki_documents_wiki_post
                        FOREIGN KEY (wiki_post_id) REFERENCES wiki_posts(id)
                        """);
            }
        }
    }
}
