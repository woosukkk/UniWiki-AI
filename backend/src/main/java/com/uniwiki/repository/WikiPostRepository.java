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
}