package com.uniwiki.service;

import com.uniwiki.dto.AnswerDto;
import com.uniwiki.entity.Answer;
import com.uniwiki.entity.Question;
import com.uniwiki.entity.User;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.QuestionRepository;
import com.uniwiki.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnswerService answerService;

    private User author;
    private User otherUser;
    private Question question;

    @BeforeEach
    void setUp() {
        author = user(1L, "작성자");
        otherUser = user(2L, "다른 사용자");
        question = new Question(author, "질문", "질문 내용");
        ReflectionTestUtils.setField(question, "id", 10L);
    }

    @Test
    void createsAnswerForQuestion() {
        AnswerDto.CreateRequest request = new AnswerDto.CreateRequest();
        ReflectionTestUtils.setField(request, "content", "답변 내용");
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(answerRepository.save(any(Answer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnswerDto.Response response = answerService.create(1L, 10L, request);

        assertThat(response.getQuestionId()).isEqualTo(10L);
        assertThat(response.getAuthorId()).isEqualTo(1L);
        assertThat(response.getContent()).isEqualTo("답변 내용");
        assertThat(response.isAccepted()).isFalse();
    }

    @Test
    void returnsAnswersForQuestionInRepositoryOrder() {
        Answer first = answer(101L, author, "첫 번째 답변");
        Answer second = answer(102L, otherUser, "두 번째 답변");
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestion_IdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(first, second));

        List<AnswerDto.Response> responses = answerService.findByQuestionId(10L);

        assertThat(responses).extracting(AnswerDto.Response::getContent)
                .containsExactly("첫 번째 답변", "두 번째 답변");
    }

    @Test
    void updatesOwnAnswer() {
        Answer answer = answer(101L, author, "기존 답변");
        AnswerDto.UpdateRequest request = new AnswerDto.UpdateRequest();
        ReflectionTestUtils.setField(request, "content", "수정된 답변");
        when(answerRepository.findById(101L)).thenReturn(Optional.of(answer));

        AnswerDto.Response response = answerService.update(1L, 101L, request);

        assertThat(response.getContent()).isEqualTo("수정된 답변");
    }

    @Test
    void rejectsUpdateByAnotherUser() {
        Answer answer = answer(101L, author, "기존 답변");
        AnswerDto.UpdateRequest request = new AnswerDto.UpdateRequest();
        ReflectionTestUtils.setField(request, "content", "수정 시도");
        when(answerRepository.findById(101L)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> answerService.update(2L, 101L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)
                );
    }

    @Test
    void deletesOwnAnswer() {
        Answer answer = answer(101L, author, "삭제할 답변");
        when(answerRepository.findById(101L)).thenReturn(Optional.of(answer));

        answerService.delete(1L, 101L);

        verify(answerRepository).delete(answer);
    }

    private User user(Long id, String nickname) {
        return User.builder()
                .id(id)
                .email(id + "@example.com")
                .password("password")
                .nickname(nickname)
                .role("USER")
                .build();
    }

    private Answer answer(Long id, User user, String content) {
        Answer answer = new Answer(question, user, content);
        ReflectionTestUtils.setField(answer, "id", id);
        return answer;
    }
}
