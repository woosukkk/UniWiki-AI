SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'questions' AND column_name = 'source_type') = 0,
    'ALTER TABLE questions ADD COLUMN source_type VARCHAR(30) NULL AFTER content',
    'SELECT 1'
);
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'questions' AND column_name = 'source_url') = 0,
    'ALTER TABLE questions ADD COLUMN source_url VARCHAR(1000) NULL AFTER source_type',
    'SELECT 1'
);
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'questions' AND column_name = 'external_like_count') = 0,
    'ALTER TABLE questions ADD COLUMN external_like_count BIGINT NOT NULL DEFAULT 0 AFTER source_url',
    'SELECT 1'
);
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

INSERT INTO categories (name, description)
SELECT '함께 만들어낸 위키', '질문과 답변을 통해 함께 검증하고 만든 위키'
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE name = '함께 만들어낸 위키'
);
