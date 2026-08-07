package com.uniwiki.service;

import com.uniwiki.entity.*;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.AnswerWikiPromotionRepository;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.WikiPostRepository;
import com.uniwiki.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class AnswerWikiPromotionServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AnswerWikiPromotionRepository promotionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private WikiPostRepository wikiPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WikiVectorSyncService vectorSyncService;

    @InjectMocks
    private AnswerWikiPromotionService promotionService;

    private User author;
    private Category category;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(1L)
                .email("author@example.com")
                .password("password")
                .nickname("작성자")
                .role("USER")
                .build();
        category = mock(Category.class);
        ReflectionTestUtils.setField(promotionService, "likeThreshold", 3L);
        ReflectionTestUtils.setField(promotionService, "categoryName", "FAQ");
    }

    @Test
    void promotesEligibleAnswerToApprovedWikiPost() {
        Question question = new Question(author, "수강신청 방법", "어떻게 하나요?");
        Answer answer = new Answer(question, author, "포털에서 신청합니다.");
        when(categoryRepository.findByName("FAQ")).thenReturn(Optional.of(category));
        when(answerRepository.findEligibleForWikiPromotion(3L)).thenReturn(List.of(answer));
        when(wikiPostRepository.save(any(WikiPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int promotedCount = promotionService.promoteEligibleAnswers();

        assertThat(promotedCount).isEqualTo(1);

        ArgumentCaptor<WikiPost> wikiCaptor = ArgumentCaptor.forClass(WikiPost.class);
        verify(wikiPostRepository).save(wikiCaptor.capture());
        WikiPost wikiPost = wikiCaptor.getValue();
        assertThat(wikiPost.getTitle()).isEqualTo("수강신청 방법");
        assertThat(wikiPost.getContent()).isEqualTo("포털에서 신청합니다.");
        assertThat(wikiPost.getAuthor()).isEqualTo(author);
        assertThat(wikiPost.getCategory()).isEqualTo(category);
        assertThat(wikiPost.getStatus()).isEqualTo(WikiPostStatus.APPROVED);

        ArgumentCaptor<AnswerWikiPromotion> promotionCaptor =
                ArgumentCaptor.forClass(AnswerWikiPromotion.class);
        verify(promotionRepository).save(promotionCaptor.capture());
        assertThat(promotionCaptor.getValue().getAnswer()).isEqualTo(answer);
        assertThat(promotionCaptor.getValue().getWikiPost()).isEqualTo(wikiPost);
        assertThat(promotionCaptor.getValue().getStatus())
                .isEqualTo(AnswerPromotionStatus.COMPLETED);
        verify(vectorSyncService).enqueueUpsert(wikiPost);
    }

    @Test
    void adminCanPromoteSpecificAnswer() {
        User admin = User.builder()
                .id(9L)
                .email("admin@example.com")
                .password("password")
                .nickname("admin")
                .role("ADMIN")
                .build();
        Question question = new Question(author, "Course registration", "How do I register?");
        Answer answer = new Answer(question, author, "Use the registration menu.");
        ReflectionTestUtils.setField(answer, "id", 7L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(promotionRepository.existsByAnswer_Id(7L)).thenReturn(false);
        when(answerRepository.findById(7L)).thenReturn(Optional.of(answer));
        when(categoryRepository.findByName("FAQ")).thenReturn(Optional.of(category));
        when(category.getId()).thenReturn(11L);
        when(category.getName()).thenReturn("FAQ");
        when(wikiPostRepository.save(any(WikiPost.class))).thenAnswer(invocation -> {
            WikiPost wikiPost = invocation.getArgument(0);
            ReflectionTestUtils.setField(wikiPost, "id", 20L);
            return wikiPost;
        });

        var response = promotionService.promoteByAdmin(9L, 7L);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getTitle()).isEqualTo("Course registration");
        verify(promotionRepository).save(any(AnswerWikiPromotion.class));
        verify(vectorSyncService).enqueueUpsert(any(WikiPost.class));
    }

    @Test
    void nonAdminCannotPromoteAnswer() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> promotionService.promoteByAdmin(1L, 7L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");

        verifyNoInteractions(answerRepository, promotionRepository, wikiPostRepository);
    }

    @Test
    void doesNothingWhenNoAnswerIsEligible() {
        when(categoryRepository.findByName("FAQ")).thenReturn(Optional.of(category));
        when(answerRepository.findEligibleForWikiPromotion(3L)).thenReturn(List.of());

        int promotedCount = promotionService.promoteEligibleAnswers();

        assertThat(promotedCount).isZero();
        verifyNoInteractions(wikiPostRepository, promotionRepository, vectorSyncService);
    }

    @Test
    void failsWhenPromotionCategoryDoesNotExist() {
        when(categoryRepository.findByName("FAQ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promotionService.promoteEligibleAnswers())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAQ");
        verifyNoInteractions(
                answerRepository,
                wikiPostRepository,
                promotionRepository,
                vectorSyncService
        );
    }
}
