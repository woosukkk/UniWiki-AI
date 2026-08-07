#!/bin/sh
set -eu

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:?DB_PORT is required}"
: "${DB_USERNAME:?DB_USERNAME is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

mysql_base="mysql --protocol=TCP --host=$DB_HOST --port=$DB_PORT --user=$DB_USERNAME --default-character-set=utf8mb4"

until MYSQL_PWD="$DB_PASSWORD" $mysql_base --execute="SELECT 1" >/dev/null 2>&1; do
  echo "Waiting for MySQL..."
  sleep 2
done

if [ "${QUERY_ONLY:-false}" != "true" ]; then
  schema_exists="$(MYSQL_PWD="$DB_PASSWORD" $mysql_base --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='uniwiki_ai' AND table_name='wiki_posts'")"
  if [ "$schema_exists" = "0" ]; then
    echo "Applying schema.sql"
    MYSQL_PWD="$DB_PASSWORD" $mysql_base < /seed/schema.sql
  else
    echo "Skipping schema.sql (schema already exists)"
  fi

  if [ "${RESET_SEEDED_WIKI:-false}" = "true" ]; then
    echo "Backing up seeded wiki posts and categories"
    MYSQL_PWD="$DB_PASSWORD" $mysql_base --database=uniwiki_ai --execute="
      CREATE TABLE IF NOT EXISTS wiki_posts_encoding_backup_20260802 LIKE wiki_posts;
      INSERT IGNORE INTO wiki_posts_encoding_backup_20260802
      SELECT post.*
      FROM wiki_posts post
      JOIN users author ON author.id = post.author_id
      WHERE author.email = 'official-source@local.invalid';
      CREATE TABLE IF NOT EXISTS categories_encoding_backup_20260802 LIKE categories;
      INSERT IGNORE INTO categories_encoding_backup_20260802 SELECT * FROM categories;
    "
    echo "Removing previously seeded official wiki posts"
    MYSQL_PWD="$DB_PASSWORD" $mysql_base --database=uniwiki_ai --execute="
      DELETE post
      FROM wiki_posts post
      JOIN users author ON author.id = post.author_id
      WHERE author.email = 'official-source@local.invalid';
    "
  fi

for file in \
  init.sql \
  migration-data-categories.sql \
  migration-wiki-content-mediumtext.sql \
  migration-wiki-view-count-default.sql \
  migration-official-topic-pinning.sql \
  migration-community-questions.sql \
  seed-sejong-software.sql \
  seed-sejong-academic-policies.sql \
  seed-sejong-student-support.sql \
  seed-sejong-yearly-wiki-drafts.sql \
  publish-sejong-yearly-wiki-drafts.sql \
  queue-seeded-wiki-sync.sql
do
  echo "Applying $file"
  MYSQL_PWD="$DB_PASSWORD" $mysql_base --database=uniwiki_ai < "/seed/$file"
done
fi

if [ "${RESET_VECTOR_JOBS:-false}" = "true" ]; then
  echo "Rebuilding vector synchronization queue"
  MYSQL_PWD="$DB_PASSWORD" $mysql_base --database=uniwiki_ai <<'SQL'
DELETE FROM wiki_vector_sync_jobs;
INSERT INTO wiki_vector_sync_jobs (
    wiki_post_id, operation, payload, status, attempt_count, created_at, updated_at
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
WHERE post.status = 'APPROVED';
SQL
fi

MYSQL_PWD="$DB_PASSWORD" $mysql_base --database=uniwiki_ai --batch --skip-column-names \
  --execute="SELECT CONCAT('wiki_posts=', COUNT(*)) FROM wiki_posts; SELECT CONCAT('approved=', COUNT(*)) FROM wiki_posts WHERE status='APPROVED'; SELECT CONCAT('vector_jobs=', COUNT(*)) FROM wiki_vector_sync_jobs;"

sleep 15
MYSQL_PWD="$DB_PASSWORD" $mysql_base --database=uniwiki_ai --batch --skip-column-names \
  --execute="SELECT CONCAT('vector_status_', LOWER(status), '=', COUNT(*)) FROM wiki_vector_sync_jobs GROUP BY status; SELECT CONCAT('vector_error=', LEFT(last_error, 1000)) FROM wiki_vector_sync_jobs WHERE last_error IS NOT NULL ORDER BY updated_at DESC LIMIT 1;"

MYSQL_PWD="$DB_PASSWORD" $mysql_base --database=uniwiki_ai --batch --skip-column-names \
  --execute="SELECT CONCAT('vector_422_error=', LEFT(last_error, 1500)) FROM wiki_vector_sync_jobs WHERE last_error LIKE '422%' LIMIT 1;"

MYSQL_PWD="$DB_PASSWORD" $mysql_base --database=uniwiki_ai --batch --skip-column-names \
  --execute="SELECT CONCAT('vector_payload_keys=', JSON_KEYS(payload), ', wikiPostIdType=', JSON_TYPE(JSON_EXTRACT(payload, '$.wikiPostId')), ', categoryIdType=', JSON_TYPE(JSON_EXTRACT(payload, '$.categoryId')), ', titleLength=', CHAR_LENGTH(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.title'))), ', contentLength=', CHAR_LENGTH(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.content')))) FROM wiki_vector_sync_jobs WHERE operation='UPSERT' LIMIT 1;"

echo "Database deployment completed."
