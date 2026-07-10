package com.uniwiki.repository;

import com.uniwiki.entity.WikiPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WikiPostRepository extends JpaRepository<WikiPost, Long> {

    // 최신순 전체 조회
    List<WikiPost> findAllByOrderByCreatedAtDesc();

    // 카테고리별 최신순 조회
    List<WikiPost> findByCategory_IdOrderByCreatedAtDesc(Long categoryId);

    // 작성자별 최신순 조회
    List<WikiPost> findByAuthor_IdOrderByCreatedAtDesc(Long authorId);

    // 제목 또는 본문에 검색어가 포함된 문서를 최신순으로 조회
    List<WikiPost>
    findByTitleContainingOrContentContainingOrderByCreatedAtDesc(
        String titleKeyword,
        String contentKeyword
    );
}
//영어 검색에서는 대소문자를 구분하지 않고, 한글 검색에도 그대로 사용할 수 있음