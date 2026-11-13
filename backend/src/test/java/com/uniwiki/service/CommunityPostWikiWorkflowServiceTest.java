package com.uniwiki.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwiki.entity.*;
import com.uniwiki.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommunityPostWikiWorkflowServiceTest {
    @Test
    void masksPersonalInformationBeforePublishingQuestionsAndComments() {
        RawCommunityPostRepository rawRepository = mock(RawCommunityPostRepository.class);
        UserRepository users = mock(UserRepository.class);
        QuestionRepository questions = mock(QuestionRepository.class);
        AnswerRepository answers = mock(AnswerRepository.class);
        CommunityPostWikiWorkflowService service = new CommunityPostWikiWorkflowService(
                rawRepository, mock(EverytimeWikiDocumentRepository.class), mock(WikiPostRepository.class),
                users, mock(WikiVectorSyncService.class), questions, answers,
                new ObjectMapper(), new LectureReviewSanitizer());
        ReflectionTestUtils.setField(service, "authorId", 1L);
        User author = User.builder().id(1L).email("source@local.invalid").role("USER").build();
        RawCommunityPost raw = new RawCommunityPost(
                "https://everytime.kr/hotarticle/v/1", "핫 게시판", "문의 test@example.com",
                "학번 20260001, 연락처 010-1234-5678 입니다.", 3, 1,
                "[\"답변 test@example.com 또는 010-1234-5678\"]");
        when(rawRepository.findUnmigrated()).thenReturn(List.of(raw));
        when(users.findById(1L)).thenReturn(Optional.of(author));
        when(questions.save(any(Question.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.processPending().accepted()).isEqualTo(1);

        ArgumentCaptor<Question> question = ArgumentCaptor.forClass(Question.class);
        verify(questions).save(question.capture());
        assertThat(question.getValue().getTitle()).contains("[이메일 삭제]").doesNotContain("test@example.com");
        assertThat(question.getValue().getContent()).contains("[학번 삭제]", "[연락처 삭제]")
                .doesNotContain("20260001", "010-1234-5678");
        ArgumentCaptor<Answer> answer = ArgumentCaptor.forClass(Answer.class);
        verify(answers).save(answer.capture());
        assertThat(answer.getValue().getContent()).contains("[이메일 삭제]", "[연락처 삭제]")
                .doesNotContain("test@example.com", "010-1234-5678");
        assertThat(raw.getSanitizedContent()).doesNotContain("20260001", "010-1234-5678");
    }
}
