USE uniwiki_ai;

-- 연도별 초안 생성기가 만든 문서만 공개한다. 다른 사용자의 DRAFT는 변경하지 않는다.
UPDATE wiki_posts
SET status = 'APPROVED',
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'DRAFT'
  AND content LIKE '%초안 ID: %';

-- 승인된 최신 본문을 벡터 저장소에 반영하도록 새 UPSERT 작업을 등록한다.
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
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM wiki_posts post
WHERE post.status = 'APPROVED'
  AND post.content LIKE '%초안 ID: %'
  AND NOT EXISTS (
      SELECT 1
      FROM wiki_vector_sync_jobs job
      WHERE job.wiki_post_id = post.id
        AND job.operation = 'UPSERT'
        AND job.payload = JSON_OBJECT(
            'wikiPostId', post.id,
            'title', post.title,
            'content', post.content,
            'categoryId', post.category_id
        )
  );
