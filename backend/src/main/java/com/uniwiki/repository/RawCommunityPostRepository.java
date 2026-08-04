package com.uniwiki.repository;

import com.uniwiki.entity.RawCommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawCommunityPostRepository extends JpaRepository<RawCommunityPost, Long> {
    boolean existsBySourceUrl(String sourceUrl);
    java.util.List<RawCommunityPost> findTop100ByIsProcessedFalseOrderByIdAsc();
}
