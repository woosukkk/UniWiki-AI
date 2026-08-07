package com.uniwiki.repository;

import com.uniwiki.entity.RawCommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RawCommunityPostRepository extends JpaRepository<RawCommunityPost, Long> {
    boolean existsBySourceUrl(String sourceUrl);
    java.util.List<RawCommunityPost> findTop100ByIsProcessedFalseOrderByIdAsc();

    @Query("""
            select r from RawCommunityPost r
            where not exists (
                select q.id from Question q
                where q.sourceType = 'EVERYTIME'
                  and q.sourceUrl = r.sourceUrl
              )
            order by r.id
            """)
    java.util.List<RawCommunityPost> findUnmigrated();
}
