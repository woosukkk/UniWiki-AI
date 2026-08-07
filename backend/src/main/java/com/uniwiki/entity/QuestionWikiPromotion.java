package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "question_wiki_promotions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_question_promotion_question", columnNames = "question_id"),
                @UniqueConstraint(name = "uq_question_promotion_wiki", columnNames = "wiki_post_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionWikiPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wiki_post_id", nullable = false)
    private WikiPost wikiPost;

    @Column(name = "promoted_at", nullable = false, updatable = false)
    private LocalDateTime promotedAt;

    public QuestionWikiPromotion(Question question, WikiPost wikiPost) {
        this.question = question;
        this.wikiPost = wikiPost;
    }

    @PrePersist
    protected void onCreate() {
        this.promotedAt = LocalDateTime.now();
    }
}
