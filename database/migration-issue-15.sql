USE uniwiki_ai;

CREATE TABLE wiki_vector_sync_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wiki_post_id BIGINT NOT NULL,
    operation ENUM('UPSERT', 'DELETE') NOT NULL,
    payload LONGTEXT,
    status ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(2000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    processed_at DATETIME,

    INDEX idx_vector_sync_retry (status, attempt_count, created_at),
    INDEX idx_vector_sync_wiki_post (wiki_post_id)
);

-- 마이그레이션 전에 존재하던 위키 문서를 최초 동기화 대상으로 등록합니다.
INSERT INTO wiki_vector_sync_jobs (
    wiki_post_id,
    operation,
    payload,
    status,
    attempt_count,
    created_at,
    updated_at
)
SELECT
    id,
    'UPSERT',
    JSON_OBJECT(
        'wikiPostId', id,
        'title', title,
        'content', content,
        'categoryId', category_id
    ),
    'PENDING',
    0,
    COALESCE(updated_at, created_at),
    COALESCE(updated_at, created_at)
FROM wiki_posts;
