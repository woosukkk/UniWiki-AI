ALTER TABLE raw_community_posts
    ADD COLUMN comments_count INT NOT NULL DEFAULT 0 AFTER likes_count,
    ADD COLUMN processing_status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL DEFAULT 'PENDING' AFTER is_processed,
    ADD COLUMN usefulness_score INT NULL AFTER processing_status,
    ADD COLUMN content_type ENUM('LECTURE_REVIEW', 'ACADEMIC', 'SCHOLARSHIP', 'FACILITIES', 'CAREER', 'CLUB_EVENT', 'SCHOOL_LIFE') NULL AFTER usefulness_score,
    ADD COLUMN sanitized_content TEXT NULL AFTER content_type,
    ADD COLUMN processing_note VARCHAR(500) NULL AFTER sanitized_content,
    ADD COLUMN processed_at DATETIME NULL AFTER crawled_at,
    ADD INDEX idx_raw_community_processing (is_processed, id);

CREATE TABLE IF NOT EXISTS everytime_wiki_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_key VARCHAR(500) NOT NULL,
    content_type ENUM('LECTURE_REVIEW', 'ACADEMIC', 'SCHOLARSHIP', 'FACILITIES', 'CAREER', 'CLUB_EVENT', 'SCHOOL_LIFE') NOT NULL,
    wiki_post_id BIGINT NOT NULL UNIQUE,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_everytime_wiki_source UNIQUE (source_key),
    CONSTRAINT fk_everytime_wiki_post FOREIGN KEY (wiki_post_id) REFERENCES wiki_posts(id) ON DELETE CASCADE
);
