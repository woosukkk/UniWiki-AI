package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "answer_wiki_promotions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_promotion_answer", columnNames = "answer_id"),
                @UniqueConstraint(name = "uq_promotion_wiki_post", columnNames = "wiki_post_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerWikiPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wiki_post_id", nullable = false)
    private WikiPost wikiPost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnswerPromotionStatus status;

    @Column(name = "promoted_at", nullable = false, updatable = false)
    private LocalDateTime promotedAt;

    public AnswerWikiPromotion(Answer answer, WikiPost wikiPost) {
        this.answer = answer;
        this.wikiPost = wikiPost;
        this.status = AnswerPromotionStatus.COMPLETED;
    }

    @PrePersist
    protected void onCreate() {
        this.promotedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AnswerPromotionStatus.COMPLETED;
        }
    }
}
