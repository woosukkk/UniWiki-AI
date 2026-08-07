package com.uniwiki.service;

import com.uniwiki.dto.AnswerDto;
import com.uniwiki.entity.Answer;
import com.uniwiki.entity.Question;
import com.uniwiki.entity.User;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.QuestionRepository;
import com.uniwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuestionWikiPromotionService questionWikiPromotionService;

    @Transactional
    public AnswerDto.Response create(
            Long loginUserId,
            Long questionId,
            AnswerDto.CreateRequest request
    ) {
        User author = findUser(loginUserId);
        Question question = findQuestion(questionId);

        Answer answer = answerRepository.save(
                new Answer(question, author, request.getContent())
        );
        questionWikiPromotionService.refreshIfPromoted(questionId);
        return AnswerDto.Response.from(answer);
    }

    public List<AnswerDto.Response> findByQuestionId(Long questionId) {
        findQuestion(questionId);

        return answerRepository.findByQuestion_IdOrderByCreatedAtAsc(questionId)
                .stream()
                .map(AnswerDto.Response::from)
                .toList();
    }

    @Transactional
    public AnswerDto.Response update(
            Long loginUserId,
            Long answerId,
            AnswerDto.UpdateRequest request
    ) {
        Answer answer = findAnswer(answerId);
        validateAuthor(answer, loginUserId);
        answer.update(request.getContent());
        questionWikiPromotionService.refreshIfPromoted(
                answer.getQuestion().getId()
        );

        return AnswerDto.Response.from(answer);
    }

    @Transactional
    public void delete(Long loginUserId, Long answerId) {
        Answer answer = findAnswer(answerId);
        validateAuthor(answer, loginUserId);
        Long questionId = answer.getQuestion().getId();
        answerRepository.delete(answer);
        answerRepository.flush();
        questionWikiPromotionService.refreshIfPromoted(questionId);
    }

    @Transactional
    public AnswerDto.Response accept(Long loginUserId, Long answerId) {
        Answer answer = findAnswer(answerId);
        Long questionId = answer.getQuestion().getId();
        Question question = questionRepository.findByIdForUpdate(questionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "질문을 찾을 수 없습니다."
                ));

        if (!question.isAuthor(loginUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "질문 작성자만 답변을 채택할 수 있습니다."
            );
        }

        answerRepository.findByQuestion_IdAndAcceptedTrue(questionId)
                .filter(acceptedAnswer -> !acceptedAnswer.getId().equals(answerId))
                .ifPresent(acceptedAnswer -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "이미 채택된 답변이 있습니다."
                    );
                });

        answer.accept();
        question.close();
        return AnswerDto.Response.from(answer);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));
    }

    private Question findQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "질문을 찾을 수 없습니다."
                ));
    }

    private Answer findAnswer(Long answerId) {
        return answerRepository.findById(answerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "답변을 찾을 수 없습니다."
                ));
    }

    private void validateAuthor(Answer answer, Long loginUserId) {
        if (!answer.isAuthor(loginUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "답변 작성자만 수정하거나 삭제할 수 있습니다."
            );
        }
    }
}
