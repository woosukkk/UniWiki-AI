SET @add_pinned_order = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'wiki_posts' AND column_name = 'pinned_order') = 0,
    'ALTER TABLE wiki_posts ADD COLUMN pinned_order INT NULL AFTER view_count',
    'SELECT 1'
);
PREPARE statement FROM @add_pinned_order;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @add_topic_key = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'official_wiki_documents' AND column_name = 'topic_key') = 0,
    'ALTER TABLE official_wiki_documents ADD COLUMN topic_key VARCHAR(255) NULL AFTER wiki_post_id',
    'SELECT 1'
);
PREPARE statement FROM @add_topic_key;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @wiki_unique_index = (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'official_wiki_documents'
      AND column_name = 'wiki_post_id'
      AND non_unique = 0
      AND index_name <> 'PRIMARY'
    LIMIT 1
);
SET @drop_wiki_unique = IF(
    @wiki_unique_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE official_wiki_documents DROP INDEX `', @wiki_unique_index, '`')
);
PREPARE statement FROM @drop_wiki_unique;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @add_topic_index = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'official_wiki_documents'
       AND index_name = 'idx_official_wiki_topic') = 0,
    'CREATE INDEX idx_official_wiki_topic ON official_wiki_documents (topic_key, wiki_post_id)',
    'SELECT 1'
);
PREPARE statement FROM @add_topic_index;
EXECUTE statement;
DEALLOCATE PREPARE statement;
