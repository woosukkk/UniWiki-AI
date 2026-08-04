USE uniwiki_ai;

-- 일시적인 AI 서비스 장애로 재시도 한도를 소진한 작업은 운영 재배포 시 다시 처리한다.
UPDATE wiki_vector_sync_jobs
SET status = 'PENDING',
    attempt_count = 0,
    last_error = NULL,
    processed_at = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'FAILED';

-- SQL 시드로 직접 추가한 문서는 애플리케이션 서비스의 동기화 큐를 거치지 않는다.
-- 아직 UPSERT 작업이 없는 문서를 벡터 저장소 최초 동기화 대상으로 등록한다.
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
    post.id,
    'UPSERT',
    JSON_OBJECT(
        'wikiPostId', post.id,
        'title', post.title,
        'content', post.content,
        'categoryId', post.category_id
    ),
    'PENDING',
    0,
    COALESCE(post.updated_at, post.created_at),
    COALESCE(post.updated_at, post.created_at)
FROM wiki_posts post
WHERE NOT EXISTS (
    SELECT 1
    FROM wiki_vector_sync_jobs job
    WHERE job.wiki_post_id = post.id
      AND job.operation = 'UPSERT'
);
