package com.uniwiki.service;

import com.uniwiki.dto.WikiPostDto;
import com.uniwiki.entity.Category;
import com.uniwiki.entity.User;
import com.uniwiki.entity.WikiPost;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiPostService {

    private final WikiPostRepository wikiPostRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

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

        return new WikiPostDto.Response(savedWikiPost);
    }

    // 위키 문서 전체 조회
    public List<WikiPostDto.ListResponse> getWikiPosts() {
        return wikiPostRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(WikiPostDto.ListResponse::new)
                .toList();
    }

    // 위키 문서 단건 조회
    @Transactional
    public WikiPostDto.Response getWikiPost(Long wikiPostId) {
        WikiPost wikiPost = findWikiPost(wikiPostId);

        wikiPost.increaseViewCount();

        return new WikiPostDto.Response(wikiPost);
    }

    // 카테고리별 위키 문서 조회
    public List<WikiPostDto.ListResponse> getWikiPostsByCategory(
            Long categoryId
    ) {
        findCategory(categoryId);

        return wikiPostRepository
                .findByCategory_IdOrderByCreatedAtDesc(categoryId)
                .stream()
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

        validateAuthor(wikiPost, userId);

        Category category = findCategory(request.getCategoryId());

        wikiPost.update(
                category,
                request.getTitle(),
                request.getContent(),
                request.getSummary(),
                request.getStatus()
        );

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

    // 검색어가 없으면 전체 문서를 반환
        if (keyword == null || keyword.isBlank()) {
                return getWikiPosts();
        }
        String trimmedKeyword = keyword.trim();
        return wikiPostRepository
                .findByTitleContainingOrContentContainingOrderByCreatedAtDesc(
                    trimmedKeyword,
                    trimmedKeyword
                 )
                .stream()
                  .map(WikiPostDto.ListResponse::new)
                  .toList();
        }
}