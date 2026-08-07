package com.uniwiki.repository;

import com.uniwiki.entity.WikiPost;
import com.uniwiki.entity.WikiPostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;

public interface WikiPostRepository extends JpaRepository<WikiPost, Long> {

    long countByStatus(WikiPostStatus status);

    @Query("""
            select p.category.id, p.category.name, p.category.description, count(p), max(p.updatedAt)
            from WikiPost p
            where p.status = :status
            group by p.category.id, p.category.name, p.category.description
            order by count(p) desc, p.category.name asc
            """)
    List<Object[]> summarizeCategoriesByStatus(WikiPostStatus status);

    @Query("select max(p.updatedAt) from WikiPost p where p.status = :status")
    LocalDateTime findLatestUpdatedAtByStatus(WikiPostStatus status);

    // 최신순 전체 조회
    List<WikiPost> findAllByOrderByCreatedAtDesc();

    List<WikiPost> findAllByStatusOrderByCreatedAtDesc(WikiPostStatus status);

    java.util.Optional<WikiPost> findByIdAndStatus(Long id, WikiPostStatus status);

    java.util.Optional<WikiPost> findByTitle(String title);

    // 카테고리별 최신순 조회
    List<WikiPost> findByCategory_IdOrderByCreatedAtDesc(Long categoryId);

    List<WikiPost> findByCategory_IdAndStatusOrderByCreatedAtDesc(
            Long categoryId,
            WikiPostStatus status
    );

    // 작성자별 최신순 조회
    List<WikiPost> findByAuthor_IdOrderByCreatedAtDesc(Long authorId);

    // 제목 또는 본문에 검색어가 포함된 문서를 최신순으로 조회
    List<WikiPost>
    findByTitleContainingOrContentContainingOrderByCreatedAtDesc(
        String titleKeyword,
        String contentKeyword
    );

    List<WikiPost> findByStatusAndTitleContainingOrStatusAndContentContainingOrderByCreatedAtDesc(
            WikiPostStatus titleStatus,
            String titleKeyword,
            WikiPostStatus contentStatus,
            String contentKeyword
    );
}
//영어 검색에서는 대소문자를 구분하지 않고, 한글 검색에도 그대로 사용할 수 있음
