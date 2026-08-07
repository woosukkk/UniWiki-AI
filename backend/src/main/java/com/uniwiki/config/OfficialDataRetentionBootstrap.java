package com.uniwiki.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(3)
@Slf4j
@ConditionalOnProperty(name = "uniwiki.official-sources.remove-before-2024", havingValue = "true")
public class OfficialDataRetentionBootstrap implements ApplicationRunner {

    private static final String LEGACY_YEAR_PATTERN =
            "(^|[^0-9])(19[0-9]{2}|20[01][0-9]|202[0-3])([^0-9]|$)";

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("CREATE TEMPORARY TABLE IF NOT EXISTS legacy_official_raw_ids "
                + "(id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TEMPORARY TABLE IF NOT EXISTS legacy_official_wiki_ids "
                + "(id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("TRUNCATE TABLE legacy_official_raw_ids");
        jdbcTemplate.execute("TRUNCATE TABLE legacy_official_wiki_ids");

        int rawDocuments = jdbcTemplate.update("""
                INSERT IGNORE INTO legacy_official_raw_ids (id)
                SELECT id
                FROM raw_official_documents
                WHERE title REGEXP ? OR source_url REGEXP ?
                """, LEGACY_YEAR_PATTERN,
                "curriculum(19[0-9]{2}|20[01][0-9]|202[0-3])\\.do");

        jdbcTemplate.update("""
                INSERT IGNORE INTO legacy_official_wiki_ids (id)
                SELECT DISTINCT wiki_post_id
                FROM official_wiki_documents
                WHERE raw_document_id IN (SELECT id FROM legacy_official_raw_ids)
                """);
        jdbcTemplate.update("""
                INSERT IGNORE INTO legacy_official_wiki_ids (id)
                SELECT post.id
                FROM wiki_posts post
                JOIN users author ON author.id = post.author_id
                WHERE author.email = 'official-source@local.invalid'
                  AND post.title REGEXP ?
                """, LEGACY_YEAR_PATTERN);

        jdbcTemplate.update("DELETE FROM official_wiki_documents "
                + "WHERE raw_document_id IN (SELECT id FROM legacy_official_raw_ids)");
        jdbcTemplate.update("DELETE FROM raw_official_documents "
                + "WHERE id IN (SELECT id FROM legacy_official_raw_ids)");
        jdbcTemplate.update("DELETE FROM wiki_vector_sync_jobs "
                + "WHERE wiki_post_id IN (SELECT id FROM legacy_official_wiki_ids)");
        jdbcTemplate.update("""
                DELETE candidate FROM legacy_official_wiki_ids candidate
                WHERE EXISTS (
                    SELECT 1 FROM official_wiki_documents link
                    WHERE link.wiki_post_id = candidate.id
                )
                """);
        int wikiPosts = jdbcTemplate.update("""
                DELETE FROM wiki_posts
                WHERE id IN (SELECT id FROM legacy_official_wiki_ids)
                """);
        jdbcTemplate.update("""
                INSERT INTO wiki_vector_sync_jobs
                    (wiki_post_id, operation, payload, status, attempt_count, created_at, updated_at)
                SELECT id, 'DELETE', NULL, 'PENDING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM legacy_official_wiki_ids
                """);
        log.info("Official data retention completed: rawDocuments={}, wikiPosts={}", rawDocuments, wikiPosts);
    }
}
