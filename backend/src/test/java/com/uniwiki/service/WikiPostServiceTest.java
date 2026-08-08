package com.uniwiki.service;

import com.uniwiki.dto.WikiPostDto;
import com.uniwiki.entity.*;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiPostServiceTest {

    @Mock
    private WikiPostRepository wikiPostRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WikiVectorSyncService vectorSyncService;

    @InjectMocks
    private WikiPostService wikiPostService;

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
        lenient().when(category.getId()).thenReturn(2L);
        lenient().when(category.getName()).thenReturn("FAQ");
    }

    @Test
    void enqueuesVectorUpsertWhenWikiIsCreated() {
        WikiPostDto.CreateRequest request = new WikiPostDto.CreateRequest();
        ReflectionTestUtils.setField(request, "categoryId", 2L);
        ReflectionTestUtils.setField(request, "title", "새 위키");
        ReflectionTestUtils.setField(request, "content", "새 위키 내용");
        ReflectionTestUtils.setField(request, "status", WikiPostStatus.APPROVED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(wikiPostRepository.save(any(WikiPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        wikiPostService.createWikiPost(1L, request);

        verify(vectorSyncService).enqueueUpsert(any(WikiPost.class));
    }

    @Test
    void doesNotEnqueueVectorUpsertWhenDraftIsCreated() {
        WikiPostDto.CreateRequest request = new WikiPostDto.CreateRequest();
        ReflectionTestUtils.setField(request, "categoryId", 2L);
        ReflectionTestUtils.setField(request, "title", "검토 중인 위키");
        ReflectionTestUtils.setField(request, "content", "초안 내용");
        ReflectionTestUtils.setField(request, "status", WikiPostStatus.DRAFT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(wikiPostRepository.save(any(WikiPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        wikiPostService.createWikiPost(1L, request);

        verifyNoInteractions(vectorSyncService);
    }

    @Test
    void publicListOnlyRequestsApprovedWikiPosts() {
        when(wikiPostRepository.findAllByStatusOrderByCreatedAtDesc(WikiPostStatus.APPROVED))
                .thenReturn(List.of());

        wikiPostService.getWikiPosts();

        verify(wikiPostRepository).findAllByStatusOrderByCreatedAtDesc(WikiPostStatus.APPROVED);
        verify(wikiPostRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void publicListKeepsPinnedPostsFirstAndThenUsesStorageOrder() {
        WikiPost first = wikiPost(10L, "First stored");
        WikiPost second = wikiPost(20L, "Second stored");
        WikiPost pinned = wikiPost(30L, "Pinned");
        ReflectionTestUtils.setField(pinned, "pinnedOrder", 1);
        when(wikiPostRepository.findAllByStatusOrderByCreatedAtDesc(WikiPostStatus.APPROVED))
                .thenReturn(List.of(second, pinned, first));

        List<WikiPostDto.ListResponse> result = wikiPostService.getWikiPosts();

        org.junit.jupiter.api.Assertions.assertEquals(
                List.of(
                        "Pinned",
                        "First stored",
                        "Second stored"
                ),
                result.stream().map(WikiPostDto.ListResponse::getTitle).toList()
        );
    }

    @Test
    void enqueuesVectorUpsertWhenWikiIsUpdated() {
        WikiPost wikiPost = wikiPost(7L);
        WikiPostDto.UpdateRequest request = new WikiPostDto.UpdateRequest();
        ReflectionTestUtils.setField(request, "categoryId", 2L);
        ReflectionTestUtils.setField(request, "title", "수정된 위키");
        ReflectionTestUtils.setField(request, "content", "수정된 내용");
        ReflectionTestUtils.setField(request, "status", WikiPostStatus.APPROVED);
        when(wikiPostRepository.findById(7L)).thenReturn(Optional.of(wikiPost));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        wikiPostService.updateWikiPost(7L, 1L, request);

        verify(vectorSyncService).enqueueUpsert(wikiPost);
    }

    @Test
    void enqueuesVectorDeleteWhenApprovedWikiBecomesDraft() {
        WikiPost wikiPost = wikiPost(7L);
        WikiPostDto.UpdateRequest request = new WikiPostDto.UpdateRequest();
        ReflectionTestUtils.setField(request, "categoryId", 2L);
        ReflectionTestUtils.setField(request, "title", "검토 중인 위키");
        ReflectionTestUtils.setField(request, "content", "검토 중인 내용");
        ReflectionTestUtils.setField(request, "status", WikiPostStatus.DRAFT);
        when(wikiPostRepository.findById(7L)).thenReturn(Optional.of(wikiPost));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        wikiPostService.updateWikiPost(7L, 1L, request);

        verify(vectorSyncService).enqueueDelete(7L);
        verify(vectorSyncService, never()).enqueueUpsert(any(WikiPost.class));
    }

    @Test
    void enqueuesVectorDeleteWhenWikiIsDeleted() {
        WikiPost wikiPost = wikiPost(7L);
        when(wikiPostRepository.findById(7L)).thenReturn(Optional.of(wikiPost));

        wikiPostService.deleteWikiPost(7L, 1L);

        verify(vectorSyncService).enqueueDelete(7L);
        verify(wikiPostRepository).delete(wikiPost);
    }

    private WikiPost wikiPost(Long id) {
        WikiPost wikiPost = new WikiPost(
                category,
                author,
                "기존 위키",
                "기존 내용",
                null,
                WikiPostStatus.APPROVED
        );
        ReflectionTestUtils.setField(wikiPost, "id", id);
        return wikiPost;
    }

    private WikiPost wikiPost(Long id, String title) {
        WikiPost wikiPost = new WikiPost(
                category,
                author,
                title,
                "테스트 내용",
                null,
                WikiPostStatus.APPROVED
        );
        ReflectionTestUtils.setField(wikiPost, "id", id);
        return wikiPost;
    }
}
