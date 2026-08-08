package com.uniwiki.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class OfficialSchemaMigration implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql")) {
                return;
            }

            List<String> uniqueIndexes = new ArrayList<>();
            boolean hasSupportingIndex = false;
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("""
                         SELECT index_name, MIN(non_unique) AS non_unique,
                                COUNT(*) AS column_count,
                                MAX(CASE WHEN seq_in_index = 1
                                         THEN LOWER(column_name) END) AS first_column
                         FROM information_schema.statistics
                         WHERE table_schema = DATABASE()
                           AND table_name = 'official_wiki_documents'
                           AND index_name <> 'PRIMARY'
                         GROUP BY index_name
                         """)) {
                while (result.next()) {
                    String indexName = result.getString("index_name");
                    boolean unique = result.getInt("non_unique") == 0;
                    int columnCount = result.getInt("column_count");
                    boolean wikiPostFirst = "wiki_post_id".equals(result.getString("first_column"));
                    if (unique && columnCount == 1 && wikiPostFirst) {
                        uniqueIndexes.add(indexName);
                    } else if (!unique && wikiPostFirst) {
                        hasSupportingIndex = true;
                    }
                }
            }

            try (Statement statement = connection.createStatement()) {
                if (!uniqueIndexes.isEmpty() && !hasSupportingIndex) {
                    statement.execute("CREATE INDEX idx_official_wiki_post_id "
                            + "ON official_wiki_documents (wiki_post_id)");
                }
                for (String indexName : uniqueIndexes) {
                    if (!indexName.matches("[A-Za-z0-9_]+")) {
                        throw new IllegalStateException("Unexpected MySQL index name: " + indexName);
                    }
                    statement.execute("ALTER TABLE official_wiki_documents DROP INDEX `"
                            + indexName + "`");
                    log.info("Removed legacy official wiki unique index: {}", indexName);
                }
            }
        }
    }
}
