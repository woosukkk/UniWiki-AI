package com.uniwiki.repository;

import com.uniwiki.entity.QuestionWikiPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestionWikiPromotionRepository
        extends JpaRepository<QuestionWikiPromotion, Long> {

    boolean existsByQuestion_Id(Long questionId);

    Optional<QuestionWikiPromotion> findByQuestion_Id(Long questionId);

    Optional<QuestionWikiPromotion> findByWikiPost_Id(Long wikiPostId);

    @Query("""
            SELECT promotion
            FROM QuestionWikiPromotion promotion
            JOIN FETCH promotion.question question
            JOIN FETCH question.author
            JOIN FETCH promotion.wikiPost
            ORDER BY promotion.promotedAt DESC, promotion.id DESC
            """)
    List<QuestionWikiPromotion> findAllForCommunityWiki();
}
