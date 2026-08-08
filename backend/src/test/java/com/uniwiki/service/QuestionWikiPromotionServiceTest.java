package com.uniwiki.service;

import com.uniwiki.entity.*;
import com.uniwiki.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionWikiPromotionServiceTest {

    @Mock UserRepository userRepository;
    @Mock QuestionRepository questionRepository;
    @Mock AnswerRepository answerRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock WikiPostRepository wikiPostRepository;
    @Mock QuestionWikiPromotionRepository promotionRepository;
    @Mock WikiVectorSyncService vectorSyncService;

    @InjectMocks QuestionWikiPromotionService promotionService;

    private User author;
    private User admin;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).email("user@sju.ac.kr").password("hash")
                .nickname("user").role("USER").build();
        admin = User.builder().id(77L).email("manager@sju.ac.kr").password("hash")
                .nickname("manager").role("ADMIN").build();
        ReflectionTestUtils.setField(promotionService, "categoryName", "함께 만든 위키");
    }

    @Test
    void adminPromotesQuestionAndAnswersToApprovedWiki() {
        Question question = new Question(author, "수강신청 질문", "신청 방법이 궁금합니다.");
        Answer answer = new Answer(question, author, "학사정보시스템에서 신청합니다.");
        Category category = new Category("함께 만든 위키", "질문에서 선정된 위키");

        when(userRepository.findById(77L)).thenReturn(Optional.of(admin));
        when(promotionRepository.existsByQuestion_Id(9L)).thenReturn(false);
        when(questionRepository.findById(9L)).thenReturn(Optional.of(question));
        when(categoryRepository.findByName("함께 만든 위키")).thenReturn(Optional.of(category));
        when(answerRepository.findByQuestion_IdOrderByCreatedAtAsc(9L)).thenReturn(List.of(answer));
        when(wikiPostRepository.save(any(WikiPost.class))).thenAnswer(invocation -> {
            WikiPost wikiPost = invocation.getArgument(0);
            ReflectionTestUtils.setField(wikiPost, "id", 30L);
            return wikiPost;
        });

        var response = promotionService.promoteByAdmin(77L, 9L);

        assertThat(response.getId()).isEqualTo(30L);
        assertThat(response.getContent()).contains("신청 방법이 궁금합니다.", "학사정보시스템에서 신청합니다.");
        assertThat(response.getStatus()).isEqualTo(WikiPostStatus.APPROVED);
        verify(promotionRepository).save(any(QuestionWikiPromotion.class));
        verify(vectorSyncService).enqueueUpsert(any(WikiPost.class));
    }

    @Test
    void regularUserCannotPromoteQuestion() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> promotionService.promoteByAdmin(1L, 9L))
                .hasMessageContaining("403 FORBIDDEN");
        verifyNoInteractions(questionRepository, wikiPostRepository, vectorSyncService);
    }

    @Test
    void createsPromotionCategoryWhenItDoesNotExist() {
        Question question = new Question(author, "학사 질문", "질문 내용");
        when(userRepository.findById(77L)).thenReturn(Optional.of(admin));
        when(promotionRepository.existsByQuestion_Id(9L)).thenReturn(false);
        when(questionRepository.findById(9L)).thenReturn(Optional.of(question));
        when(categoryRepository.findByName("함께 만든 위키")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.findByQuestion_IdOrderByCreatedAtAsc(9L)).thenReturn(List.of());
        when(wikiPostRepository.save(any(WikiPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        promotionService.promoteByAdmin(77L, 9L);

        verify(categoryRepository).save(argThat(category ->
                "함께 만든 위키".equals(category.getName())));
    }
}
