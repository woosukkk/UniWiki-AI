package com.uniwiki.service;

import com.uniwiki.dto.AnswerLikeDto;
import com.uniwiki.entity.Like;
import com.uniwiki.entity.LikeTargetType;
import com.uniwiki.entity.User;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.LikeRepository;
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
class AnswerLikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnswerLikeService answerLikeService;

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
    void addsAnswerLike() {
        when(answerRepository.existsById(20L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.ANSWER, 20L
        )).thenReturn(false);
        when(likeRepository.countByTargetTypeAndTargetId(
                LikeTargetType.ANSWER, 20L
        )).thenReturn(1L);

        AnswerLikeDto.Response response = answerLikeService.add(1L, 20L);

        assertThat(response.getAnswerId()).isEqualTo(20L);
        assertThat(response.getLikeCount()).isEqualTo(1L);
        assertThat(response.isLiked()).isTrue();
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    void rejectsDuplicateLike() {
        when(answerRepository.existsById(20L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.ANSWER, 20L
        )).thenReturn(true);

        assertThatThrownBy(() -> answerLikeService.add(1L, 20L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
                );
    }

    @Test
    void removesAnswerLike() {
        Like like = new Like(user, LikeTargetType.ANSWER, 20L);
        when(answerRepository.existsById(20L)).thenReturn(true);
        when(likeRepository.findByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.ANSWER, 20L
        )).thenReturn(Optional.of(like));

        answerLikeService.remove(1L, 20L);

        verify(likeRepository).delete(like);
    }

    @Test
    void returnsAnswerLikeCount() {
        when(answerRepository.existsById(20L)).thenReturn(true);
        when(likeRepository.countByTargetTypeAndTargetId(
                LikeTargetType.ANSWER, 20L
        )).thenReturn(4L);

        AnswerLikeDto.Response response = answerLikeService.getCount(20L);

        assertThat(response.getLikeCount()).isEqualTo(4L);
        assertThat(response.isLiked()).isFalse();
    }

    @Test
    void returnsAuthenticatedUsersAnswerLikeStatus() {
        when(answerRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.ANSWER, 10L
        )).thenReturn(true);

        assertThat(answerLikeService.getStatus(1L, 10L).isLiked()).isTrue();
    }
}
