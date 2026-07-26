package com.uniwiki.service;

import com.uniwiki.entity.*;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.AnswerWikiPromotionRepository;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.WikiPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerWikiPromotionService {

    private static final String PROMOTION_SUMMARY =
            "질문 게시판의 우수 답변에서 자동 등록된 위키 문서입니다.";

    private final AnswerRepository answerRepository;
    private final AnswerWikiPromotionRepository promotionRepository;
    private final CategoryRepository categoryRepository;
    private final WikiPostRepository wikiPostRepository;
    private final WikiVectorSyncService vectorSyncService;

    @Value("${uniwiki.promotion.like-threshold:3}")
    private long likeThreshold;

    @Value("${uniwiki.promotion.category-name:FAQ}")
    private String categoryName;

    @Transactional
    public int promoteEligibleAnswers() {
        Category category = findPromotionCategory();
        List<Answer> answers = answerRepository.findEligibleForWikiPromotion(likeThreshold);

        for (Answer answer : answers) {
            promote(answer, category);
        }

        return answers.size();
    }

    private void promote(Answer answer, Category category) {
        WikiPost wikiPost = new WikiPost(
                category,
                answer.getAuthor(),
                answer.getQuestion().getTitle(),
                answer.getContent(),
                PROMOTION_SUMMARY,
                WikiPostStatus.APPROVED
        );

        WikiPost savedWikiPost = wikiPostRepository.save(wikiPost);
        promotionRepository.save(new AnswerWikiPromotion(answer, savedWikiPost));
        vectorSyncService.enqueueUpsert(savedWikiPost);
    }

    private Category findPromotionCategory() {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new IllegalStateException(
                        "자동 위키 등록 카테고리를 찾을 수 없습니다: " + categoryName
                ));
    }
}
