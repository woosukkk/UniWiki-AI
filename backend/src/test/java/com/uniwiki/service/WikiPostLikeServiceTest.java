package com.uniwiki.service;

import com.uniwiki.dto.WikiPostLikeDto;
import com.uniwiki.entity.Like;
import com.uniwiki.entity.LikeTargetType;
import com.uniwiki.entity.User;
import com.uniwiki.repository.LikeRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiPostLikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private WikiPostRepository wikiPostRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WikiPostLikeService wikiPostLikeService;

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
    void addsWikiPostLike() {
        when(wikiPostRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.WIKI_POST, 10L
        )).thenReturn(false);
        when(likeRepository.countByTargetTypeAndTargetId(
                LikeTargetType.WIKI_POST, 10L
        )).thenReturn(1L);

        WikiPostLikeDto.Response response = wikiPostLikeService.add(1L, 10L);

        assertThat(response.getWikiPostId()).isEqualTo(10L);
        assertThat(response.getLikeCount()).isEqualTo(1L);
        assertThat(response.isLiked()).isTrue();
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    void rejectsDuplicateWikiPostLike() {
        when(wikiPostRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.WIKI_POST, 10L
        )).thenReturn(true);

        assertThatThrownBy(() -> wikiPostLikeService.add(1L, 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
                );
        verify(likeRepository, never()).save(any());
    }

    @Test
    void removesWikiPostLike() {
        Like like = new Like(user, LikeTargetType.WIKI_POST, 10L);
        when(wikiPostRepository.existsById(10L)).thenReturn(true);
        when(likeRepository.findByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.WIKI_POST, 10L
        )).thenReturn(Optional.of(like));

        wikiPostLikeService.remove(1L, 10L);

        verify(likeRepository).delete(like);
    }

    @Test
    void returnsPublicWikiPostLikeCount() {
        when(wikiPostRepository.existsById(10L)).thenReturn(true);
        when(likeRepository.countByTargetTypeAndTargetId(
                LikeTargetType.WIKI_POST, 10L
        )).thenReturn(3L);

        WikiPostLikeDto.Response response = wikiPostLikeService.getCount(10L);

        assertThat(response.getLikeCount()).isEqualTo(3L);
        assertThat(response.isLiked()).isFalse();
    }

    @Test
    void returnsAuthenticatedUsersWikiPostLikeStatus() {
        when(wikiPostRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                1L, LikeTargetType.WIKI_POST, 10L
        )).thenReturn(true);
        when(likeRepository.countByTargetTypeAndTargetId(
                LikeTargetType.WIKI_POST, 10L
        )).thenReturn(3L);

        WikiPostLikeDto.Response response = wikiPostLikeService.getStatus(1L, 10L);

        assertThat(response.getLikeCount()).isEqualTo(3L);
        assertThat(response.isLiked()).isTrue();
    }

    @Test
    void rejectsLikeForMissingWikiPost() {
        when(wikiPostRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> wikiPostLikeService.add(1L, 99L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
                );
    }
}
