package com.uniwiki.service;

import com.uniwiki.dto.QuestionDto;
import com.uniwiki.entity.Question;
import com.uniwiki.entity.User;
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
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    // 질문 생성
    @Transactional
    public QuestionDto.Response create(
            Long loginUserId,
            QuestionDto.CreateRequest request
    ) {
        // JWT에서 전달받은 사용자 ID로 작성자 조회
        User author = findUser(loginUserId);

        Question question = new Question(
                author,
                request.getTitle(),
                request.getContent()
        );

        Question savedQuestion = questionRepository.save(question);

        return QuestionDto.Response.from(savedQuestion);
    }

    // 질문 전체 조회
    public List<QuestionDto.Response> findAll() {
        return questionRepository.findAll()
                .stream()
                .map(QuestionDto.Response::from)
                .toList();
    }

    // 질문 단건 조회
    public QuestionDto.Response findById(Long questionId) {
        Question question = findQuestion(questionId);

        return QuestionDto.Response.from(question);
    }

    // 질문 수정
    @Transactional
    public QuestionDto.Response update(
            Long loginUserId,
            Long questionId,
            QuestionDto.UpdateRequest request
    ) {
        Question question = findQuestion(questionId);

        // 작성자가 아닌 사용자는 수정할 수 없음
        validateAuthor(question, loginUserId);

        question.update(
                request.getTitle(),
                request.getContent()
        );

        return QuestionDto.Response.from(question);
    }

    // 질문 삭제
    @Transactional
    public void delete(Long loginUserId, Long questionId) {
        Question question = findQuestion(questionId);

        // 작성자가 아닌 사용자는 삭제할 수 없음
        validateAuthor(question, loginUserId);

        questionRepository.delete(question);
    }

    // 사용자 조회 공통 메서드
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));
    }

    // 질문 조회 공통 메서드
    private Question findQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "질문을 찾을 수 없습니다."
                ));
    }

    // 질문 작성자 권한 검증
    private void validateAuthor(Question question, Long loginUserId) {
        if (!question.isAuthor(loginUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "질문 작성자만 수정하거나 삭제할 수 있습니다."
            );
        }
    }
}