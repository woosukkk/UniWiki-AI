CREATE TABLE IF NOT EXISTS official_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL UNIQUE,
    list_url VARCHAR(1000) NOT NULL,
    article_link_selector VARCHAR(500) NOT NULL,
    title_selector VARCHAR(500) NOT NULL,
    content_selector VARCHAR(500) NOT NULL,
    auto_publish BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_checked_at DATETIME,
    last_success_at DATETIME,
    last_error VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_official_source_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS raw_official_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    official_source_id BIGINT NOT NULL,
    source_url VARCHAR(500) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    processing_status ENUM('PENDING', 'DRAFTED', 'PUBLISHED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    first_collected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_collected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_raw_official_source_url UNIQUE (official_source_id, source_url),
    CONSTRAINT fk_raw_official_source FOREIGN KEY (official_source_id) REFERENCES official_sources(id)
);

CREATE TABLE IF NOT EXISTS official_wiki_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    raw_document_id BIGINT NOT NULL UNIQUE,
    wiki_post_id BIGINT NOT NULL UNIQUE,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_official_wiki_raw FOREIGN KEY (raw_document_id) REFERENCES raw_official_documents(id),
    CONSTRAINT fk_official_wiki_post FOREIGN KEY (wiki_post_id) REFERENCES wiki_posts(id) ON DELETE CASCADE
);
