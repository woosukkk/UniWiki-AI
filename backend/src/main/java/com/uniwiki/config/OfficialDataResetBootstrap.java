package com.uniwiki.config;

import com.uniwiki.service.AiVectorStoreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
@Slf4j
@ConditionalOnProperty(name = "uniwiki.official-sources.reset-enabled", havingValue = "true")
public class OfficialDataResetBootstrap implements ApplicationRunner {
    private static final int BATCH_SIZE = 10;

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final AiVectorStoreClient vectorStoreClient;

    @Override
    public void run(ApplicationArguments args) {
        List<Long> wikiIds = jdbcTemplate.queryForList("""
                SELECT DISTINCT link.wiki_post_id
                FROM official_wiki_documents link
                JOIN wiki_posts post ON post.id = link.wiki_post_id
                JOIN users author ON author.id = post.author_id
                WHERE author.email = 'official-source@local.invalid'
                  AND post.id NOT IN (1, 2, 3)
                  AND post.pinned_order IS NULL
                ORDER BY link.wiki_post_id
                """, Long.class);
        wikiIds.forEach(vectorStoreClient::delete);
        for (int start = 0; start < wikiIds.size(); start += BATCH_SIZE) {
            List<Long> batch = wikiIds.subList(start, Math.min(start + BATCH_SIZE, wikiIds.size()));
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> deleteBatch(batch));
        }
        log.info("Official data reset completed: wikiPosts={}", wikiIds.size());
    }

    private void deleteBatch(List<Long> wikiIds) {
        String placeholders = placeholders(wikiIds.size());
        Object[] wikiIdArgs = wikiIds.toArray();
        List<Long> rawIds = jdbcTemplate.queryForList(
                "SELECT raw_document_id FROM official_wiki_documents WHERE wiki_post_id IN (" + placeholders + ")",
                Long.class, wikiIdArgs);
        jdbcTemplate.update("DELETE FROM likes WHERE target_type = 'WIKI_POST' AND target_id IN (" + placeholders + ")", wikiIdArgs);
        jdbcTemplate.update("DELETE FROM wiki_vector_sync_jobs WHERE wiki_post_id IN (" + placeholders + ")", wikiIdArgs);
        if (!rawIds.isEmpty()) {
            jdbcTemplate.update("DELETE FROM official_attachments WHERE raw_document_id IN (" + placeholders(rawIds.size()) + ")", rawIds.toArray());
        }
        jdbcTemplate.update("DELETE FROM official_wiki_documents WHERE wiki_post_id IN (" + placeholders + ")", wikiIdArgs);
        if (!rawIds.isEmpty()) {
            jdbcTemplate.update("DELETE FROM raw_official_documents WHERE id IN (" + placeholders(rawIds.size()) + ")", rawIds.toArray());
        }
        jdbcTemplate.update("DELETE FROM wiki_posts WHERE id IN (" + placeholders + ")", wikiIdArgs);
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }
}
