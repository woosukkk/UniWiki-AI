USE uniwiki_ai;

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
