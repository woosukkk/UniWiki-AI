package com.uniwiki.service;

import com.uniwiki.entity.*;
import com.uniwiki.dto.WikiPostDto;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.AnswerWikiPromotionRepository;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.WikiPostRepository;
import com.uniwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    private final UserRepository userRepository;
    private final WikiVectorSyncService vectorSyncService;

    @Value("${uniwiki.promotion.like-threshold:3}")
    private long likeThreshold;

    @Value("${uniwiki.promotion.category-name:함께 만든 위키}")
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

    @Transactional
    public WikiPostDto.Response promoteByAdmin(Long adminUserId, Long answerId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."
                ));
        if (!"ADMIN".equals(admin.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "관리자만 위키를 선정할 수 있습니다."
            );
        }
        if (promotionRepository.existsByAnswer_Id(answerId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "이미 함께 만든 위키로 선정된 답변입니다."
            );
        }

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다."
                ));
        WikiPost wikiPost = promote(answer, findPromotionCategory());
        return new WikiPostDto.Response(wikiPost);
    }

    private WikiPost promote(Answer answer, Category category) {
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
        return savedWikiPost;
    }

    private Category findPromotionCategory() {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new IllegalStateException(
                        "자동 위키 등록 카테고리를 찾을 수 없습니다: " + categoryName
                ));
    }
}
