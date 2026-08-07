ALTER TABLE questions
    ADD COLUMN source_type VARCHAR(30) NULL AFTER content,
    ADD COLUMN source_url VARCHAR(1000) NULL AFTER source_type,
    ADD COLUMN external_like_count BIGINT NOT NULL DEFAULT 0 AFTER source_url;

INSERT INTO categories (name, description)
SELECT '함께 만들어낸 위키', '질문과 답변을 통해 함께 검증하고 만든 위키'
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE name = '함께 만들어낸 위키'
);
