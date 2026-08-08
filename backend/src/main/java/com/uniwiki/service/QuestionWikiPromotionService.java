package com.uniwiki.service;

import com.uniwiki.dto.WikiPostDto;
import com.uniwiki.entity.*;
import com.uniwiki.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionWikiPromotionService {

    private static final String SUMMARY =
            "질문 게시판에서 관리자가 선정한 질문과 답변을 함께 정리한 위키입니다.";

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;
    private final WikiPostRepository wikiPostRepository;
    private final QuestionWikiPromotionRepository promotionRepository;
    private final WikiVectorSyncService vectorSyncService;

    @Value("${uniwiki.promotion.category-name:함께 만든 위키}")
    private String categoryName;

    @Transactional
    public WikiPostDto.Response promoteByAdmin(Long adminUserId, Long questionId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"ADMIN".equals(admin.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "관리자만 질문을 위키로 선정할 수 있습니다."
            );
        }
        if (promotionRepository.existsByQuestion_Id(questionId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "이미 함께 만든 위키로 선정된 질문입니다."
            );
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."
                ));
        Category category = categoryRepository.findByName(categoryName)
                .orElseGet(() -> categoryRepository.save(new Category(
                        categoryName,
                        "질문 게시판에서 관리자가 선정한 질문과 답변"
                )));
        List<Answer> answers = answerRepository
                .findByQuestion_IdOrderByCreatedAtAsc(questionId);
        WikiPost wikiPost = wikiPostRepository.save(new WikiPost(
                category,
                question.getAuthor(),
                question.getTitle(),
                buildContent(question, answers),
                SUMMARY,
                WikiPostStatus.APPROVED
        ));
        promotionRepository.save(new QuestionWikiPromotion(question, wikiPost));
        vectorSyncService.enqueueUpsert(wikiPost);
        return new WikiPostDto.Response(wikiPost);
    }

    @Transactional
    public void refreshIfPromoted(Long questionId) {
        promotionRepository.findByQuestion_Id(questionId).ifPresent(promotion -> {
            Question question = promotion.getQuestion();
            WikiPost wikiPost = promotion.getWikiPost();
            wikiPost.update(
                    wikiPost.getCategory(),
                    question.getTitle(),
                    buildContent(
                            question,
                            answerRepository.findByQuestion_IdOrderByCreatedAtAsc(questionId)
                    ),
                    SUMMARY,
                    WikiPostStatus.APPROVED
            );
            vectorSyncService.enqueueUpsert(wikiPost);
        });
    }

    private String buildContent(Question question, List<Answer> answers) {
        StringBuilder content = new StringBuilder()
                .append("질문\n")
                .append(question.getContent().trim());
        if (!answers.isEmpty()) {
            content.append("\n\n답변\n");
            for (int index = 0; index < answers.size(); index++) {
                content.append(index + 1)
                        .append(". ")
                        .append(answers.get(index).getContent().trim())
                        .append("\n");
            }
        }
        return content.toString().trim();
    }
}
