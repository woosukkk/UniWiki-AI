package com.uniwiki.repository;

import com.uniwiki.entity.AnswerWikiPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnswerWikiPromotionRepository
        extends JpaRepository<AnswerWikiPromotion, Long> {

    Optional<AnswerWikiPromotion> findByWikiPost_Id(Long wikiPostId);

    boolean existsByAnswer_Id(Long answerId);

    @Query("""
            SELECT promotion
            FROM AnswerWikiPromotion promotion
            JOIN FETCH promotion.answer answer
            JOIN FETCH answer.question question
            JOIN FETCH question.author
            JOIN FETCH promotion.wikiPost
            ORDER BY promotion.promotedAt DESC, promotion.id DESC
            """)
    List<AnswerWikiPromotion> findAllForCommunityWiki();
}
