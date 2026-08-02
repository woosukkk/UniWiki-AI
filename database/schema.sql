CREATE DATABASE uniwiki_ai
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE uniwiki_ai;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wiki_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    summary TEXT,
    status ENUM('DRAFT', 'PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED',
    view_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_wiki_category
        FOREIGN KEY (category_id) REFERENCES categories(id),

    CONSTRAINT fk_wiki_author
        FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE wiki_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wiki_post_id BIGINT NOT NULL,
    editor_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    version_number INT NOT NULL,
    edit_reason VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_version_post
        FOREIGN KEY (wiki_post_id) REFERENCES wiki_posts(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_version_editor
        FOREIGN KEY (editor_id) REFERENCES users(id)
);

CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status ENUM('OPEN', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_question_author
        FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_answer_question
        FOREIGN KEY (question_id) REFERENCES questions(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_answer_author
        FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wiki_post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_comment_post
        FOREIGN KEY (wiki_post_id) REFERENCES wiki_posts(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_comment_author
        FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type ENUM('WIKI_POST', 'QUESTION', 'ANSWER') NOT NULL,
    target_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_like_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT uq_like UNIQUE (user_id, target_type, target_id)
);

CREATE TABLE answer_wiki_promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_id BIGINT NOT NULL UNIQUE,
    wiki_post_id BIGINT NOT NULL UNIQUE,
    status ENUM('COMPLETED') NOT NULL DEFAULT 'COMPLETED',
    promoted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_promotion_answer
        FOREIGN KEY (answer_id) REFERENCES answers(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_promotion_wiki_post
        FOREIGN KEY (wiki_post_id) REFERENCES wiki_posts(id)
        ON DELETE CASCADE
);

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

CREATE TABLE raw_lecture_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_url VARCHAR(1000) NOT NULL,
    course_name VARCHAR(200) NOT NULL,
    professor VARCHAR(100) NOT NULL,
    star_rating INT NOT NULL,
    likes_count INT NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing_status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    sanitized_content TEXT,
    processing_note VARCHAR(500),
    crawled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,

    INDEX idx_raw_lecture_processing (is_processed, id),
    INDEX idx_raw_lecture_course_professor (course_name, professor)
);

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
