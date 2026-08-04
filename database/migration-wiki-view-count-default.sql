USE uniwiki_ai;

ALTER TABLE wiki_posts
    MODIFY COLUMN view_count BIGINT NOT NULL DEFAULT 0;
