package com.uniwiki.repository;

import com.uniwiki.entity.RawCommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RawCommunityPostRepository extends JpaRepository<RawCommunityPost, Long> {
    boolean existsBySourceUrl(String sourceUrl);
    java.util.Optional<RawCommunityPost> findBySourceUrl(String sourceUrl);
    java.util.List<RawCommunityPost> findTop100ByIsProcessedFalseOrderByIdAsc();

    @Query("""
            select r from RawCommunityPost r
            where r.isProcessed = false
              and (r.processingStatus <> com.uniwiki.entity.CommunityPostProcessingStatus.REJECTED
                   or r.processingNote <> 'ABUSIVE_CONTENT')
            order by r.id
            """)
    java.util.List<RawCommunityPost> findUnmigrated();
}
