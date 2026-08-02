ALTER TABLE raw_lecture_evaluations
    ADD COLUMN processing_status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL DEFAULT 'PENDING' AFTER is_processed,
    ADD COLUMN sanitized_content TEXT AFTER processing_status,
    ADD COLUMN processing_note VARCHAR(500) AFTER sanitized_content,
    ADD COLUMN processed_at DATETIME AFTER crawled_at,
    ADD INDEX idx_raw_lecture_processing (is_processed, id),
    ADD INDEX idx_raw_lecture_course_professor (course_name, professor);

UPDATE raw_lecture_evaluations
SET is_processed = FALSE,
    processing_status = 'PENDING',
    sanitized_content = NULL,
    processing_note = NULL,
    processed_at = NULL;

CREATE TABLE lecture_review_wiki_drafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(200) NOT NULL,
    professor VARCHAR(100) NOT NULL,
    wiki_post_id BIGINT NOT NULL UNIQUE,
    included_review_count INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_lecture_review_course_professor UNIQUE (course_name, professor),
    CONSTRAINT fk_lecture_review_wiki_post
        FOREIGN KEY (wiki_post_id) REFERENCES wiki_posts(id)
        ON DELETE CASCADE
);
