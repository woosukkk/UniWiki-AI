package com.uniwiki.service;

import com.uniwiki.dto.LikeDto;
import com.uniwiki.entity.Like;
import com.uniwiki.entity.LikeTargetType;
import com.uniwiki.entity.User;
import com.uniwiki.repository.LikeRepository;
import com.uniwiki.repository.QuestionRepository;
import com.uniwiki.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionLikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuestionLikeService questionLikeService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("password")
                .nickname("사용자")
                .role("USER")
                .build();
    }

    @Test
    void addsQuestionLike() {
        when(questionRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.QUESTION, 10L
        )).thenReturn(false);
        when(likeRepository.countByTargetTypeAndTargetId(
                LikeTargetType.QUESTION, 10L
        )).thenReturn(1L);

        LikeDto.Response response = questionLikeService.add(1L, 10L);

        assertThat(response.getQuestionId()).isEqualTo(10L);
        assertThat(response.getLikeCount()).isEqualTo(1L);
        assertThat(response.isLiked()).isTrue();
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    void rejectsDuplicateLike() {
        when(questionRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.QUESTION, 10L
        )).thenReturn(true);

        assertThatThrownBy(() -> questionLikeService.add(1L, 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
                );
        verify(likeRepository, never()).save(any());
    }

    @Test
    void removesQuestionLike() {
        Like like = new Like(user, LikeTargetType.QUESTION, 10L);
        when(questionRepository.existsById(10L)).thenReturn(true);
        when(likeRepository.findByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.QUESTION, 10L
        )).thenReturn(Optional.of(like));

        questionLikeService.remove(1L, 10L);

        verify(likeRepository).delete(like);
    }

    @Test
    void rejectsRemovingMissingLike() {
        when(questionRepository.existsById(10L)).thenReturn(true);
        when(likeRepository.findByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.QUESTION, 10L
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionLikeService.remove(1L, 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
                );
    }

    @Test
    void returnsQuestionLikeCount() {
        when(questionRepository.existsById(10L)).thenReturn(true);
        when(likeRepository.countByTargetTypeAndTargetId(
                LikeTargetType.QUESTION, 10L
        )).thenReturn(3L);

        LikeDto.Response response = questionLikeService.getCount(10L);

        assertThat(response.getLikeCount()).isEqualTo(3L);
        assertThat(response.isLiked()).isFalse();
    }
}
