package com.uniwiki.service;

import com.uniwiki.dto.WikiPostDto;
import com.uniwiki.entity.Category;
import com.uniwiki.entity.User;
import com.uniwiki.entity.WikiPost;
import com.uniwiki.entity.WikiPostStatus;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import com.uniwiki.repository.LectureReviewWikiDraftRepository;
import com.uniwiki.repository.EverytimeWikiDocumentRepository;
import com.uniwiki.repository.QuestionWikiPromotionRepository;
import com.uniwiki.repository.AnswerWikiPromotionRepository;
import com.uniwiki.entity.EverytimeContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiPostService {

    private final WikiPostRepository wikiPostRepository;
    private final LectureReviewWikiDraftRepository lectureReviewWikiDraftRepository;
    private final EverytimeWikiDocumentRepository everytimeWikiDocumentRepository;
    private final QuestionWikiPromotionRepository questionWikiPromotionRepository;
    private final AnswerWikiPromotionRepository answerWikiPromotionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WikiVectorSyncService vectorSyncService;

    // 위키 문서 생성
    @Transactional
    public WikiPostDto.Response createWikiPost(
            Long userId,
            WikiPostDto.CreateRequest request
    ) {
        User author = findUser(userId);
        Category category = findCategory(request.getCategoryId());

        WikiPost wikiPost = new WikiPost(
                category,
                author,
                request.getTitle(),
                request.getContent(),
                request.getSummary(),
                request.getStatus()
        );

        WikiPost savedWikiPost = wikiPostRepository.save(wikiPost);
        if (savedWikiPost.getStatus() == WikiPostStatus.APPROVED) {
            vectorSyncService.enqueueUpsert(savedWikiPost);
        }

        return new WikiPostDto.Response(savedWikiPost);
    }

    // 위키 문서 전체 조회
    public List<WikiPostDto.ListResponse> getWikiPosts() {
        return wikiPostRepository.findAllByStatusOrderByCreatedAtDesc(WikiPostStatus.APPROVED)
                .stream()
                .sorted(storageOrderComparator())
                .map(WikiPostDto.ListResponse::new)
                .toList();
    }

    // 위키 문서 단건 조회
    @Transactional
    public WikiPostDto.Response getWikiPost(Long wikiPostId) {
        WikiPost wikiPost = findApprovedWikiPost(wikiPostId);

        wikiPost.increaseViewCount();

        return new WikiPostDto.Response(wikiPost);
    }

    // 카테고리별 위키 문서 조회
    public List<WikiPostDto.ListResponse> getWikiPostsByCategory(
            Long categoryId
    ) {
        findCategory(categoryId);

        return wikiPostRepository
                .findByCategory_IdAndStatusOrderByCreatedAtDesc(categoryId, WikiPostStatus.APPROVED)
                .stream()
                .sorted(storageOrderComparator())
                .map(WikiPostDto.ListResponse::new)
                .toList();
    }

    // 로그인 사용자가 작성한 문서 조회
    public List<WikiPostDto.ListResponse> getMyWikiPosts(Long userId) {
        findUser(userId);

        return wikiPostRepository
                .findByAuthor_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(WikiPostDto.ListResponse::new)
                .toList();
    }

    // 위키 문서 수정
    @Transactional
    public WikiPostDto.Response updateWikiPost(
            Long wikiPostId,
            Long userId,
            WikiPostDto.UpdateRequest request
    ) {
        WikiPost wikiPost = findWikiPost(wikiPostId);
        boolean wasApproved = wikiPost.getStatus() == WikiPostStatus.APPROVED;

        validateAuthor(wikiPost, userId);

        Category category = findCategory(request.getCategoryId());

        wikiPost.update(
                category,
                request.getTitle(),
                request.getContent(),
                request.getSummary(),
                request.getStatus()
        );
        if (wikiPost.getStatus() == WikiPostStatus.APPROVED) {
            vectorSyncService.enqueueUpsert(wikiPost);
        } else if (wasApproved) {
            vectorSyncService.enqueueDelete(wikiPostId);
        }

        return new WikiPostDto.Response(wikiPost);
    }

    // 위키 문서 삭제
    @Transactional
    public void deleteWikiPost(
            Long wikiPostId,
            Long userId
    ) {
        WikiPost wikiPost = findWikiPost(wikiPostId);

        validateAuthor(wikiPost, userId);

        vectorSyncService.enqueueDelete(wikiPostId);
        questionWikiPromotionRepository.findByWikiPost_Id(wikiPostId)
                .ifPresent(questionWikiPromotionRepository::delete);
        answerWikiPromotionRepository.findByWikiPost_Id(wikiPostId)
                .ifPresent(answerWikiPromotionRepository::delete);
        wikiPostRepository.delete(wikiPost);
    }

    private WikiPost findWikiPost(Long wikiPostId) {
        return wikiPostRepository.findById(wikiPostId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 위키 문서입니다."
                        )
                );
    }

    private WikiPost findApprovedWikiPost(Long wikiPostId) {
        return wikiPostRepository.findByIdAndStatus(wikiPostId, WikiPostStatus.APPROVED)
                .orElseThrow(() -> new IllegalArgumentException("공개된 위키 문서가 아닙니다."));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 카테고리입니다."
                        )
                );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 사용자입니다."
                        )
                );
    }

    // 작성자 본인인지 확인
    private void validateAuthor(
            WikiPost wikiPost,
            Long userId
    ) {
        Long authorId = wikiPost.getAuthor().getId();

        if (!authorId.equals(userId)) {
            throw new IllegalArgumentException(
                    "위키 문서 작성자만 수정하거나 삭제할 수 있습니다."
            );
        }
    }

    // 제목 또는 본문으로 위키 문서 검색
    public List<WikiPostDto.ListResponse> searchWikiPosts(String keyword) {
        return searchWikiPosts(keyword, "ALL", null);
    }

    public List<WikiPostDto.ListResponse> searchWikiPosts(
            String keyword,
            String source,
            String contentType
    ) {

        String normalizedSource = source == null ? "ALL" : source.trim().toUpperCase();
        if (!Set.of("ALL", "OFFICIAL", "EVERYTIME").contains(normalizedSource)) {
            throw new IllegalArgumentException("지원하지 않는 위키 출처입니다: " + source);
        }
        List<WikiPost> posts;
        if (keyword == null || keyword.isBlank()) {
            posts = wikiPostRepository.findAllByStatusOrderByCreatedAtDesc(WikiPostStatus.APPROVED);
        } else {
            String trimmedKeyword = keyword.trim();
            posts = wikiPostRepository
                    .findByStatusAndTitleContainingOrStatusAndContentContainingOrderByCreatedAtDesc(
                            WikiPostStatus.APPROVED,
                            trimmedKeyword,
                            WikiPostStatus.APPROVED,
                            trimmedKeyword
                    );
        }

        Set<Long> lectureReviewIds = new HashSet<>(lectureReviewWikiDraftRepository.findAllWikiPostIds());
        Set<Long> communityIds = new HashSet<>(everytimeWikiDocumentRepository.findAllWikiPostIds());
        Set<Long> selectedEverytimeIds = new HashSet<>();
        if (contentType == null || contentType.isBlank()) {
            selectedEverytimeIds.addAll(lectureReviewIds);
            selectedEverytimeIds.addAll(communityIds);
        } else {
            EverytimeContentType requestedType;
            try {
                requestedType = EverytimeContentType.valueOf(contentType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("지원하지 않는 에브리타임 자료 유형입니다: " + contentType);
            }
            if (requestedType == EverytimeContentType.LECTURE_REVIEW) {
                selectedEverytimeIds.addAll(lectureReviewIds);
            } else {
                selectedEverytimeIds.addAll(everytimeWikiDocumentRepository.findWikiPostIdsByContentType(requestedType));
            }
        }
        Set<Long> allEverytimeIds = new HashSet<>(lectureReviewIds);
        allEverytimeIds.addAll(communityIds);
        return posts
                .stream()
                .filter(post -> switch (normalizedSource) {
                    case "OFFICIAL" -> !allEverytimeIds.contains(post.getId());
                    case "EVERYTIME" -> selectedEverytimeIds.contains(post.getId());
                    default -> true;
                })
                .sorted(storageOrderComparator())
                .map(WikiPostDto.ListResponse::new)
                .toList();
        }

    private Comparator<WikiPost> storageOrderComparator() {
        return Comparator
                .comparingInt((WikiPost post) -> post.getPinnedOrder() == null
                        ? Integer.MAX_VALUE : post.getPinnedOrder())
                .thenComparing(WikiPost::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
