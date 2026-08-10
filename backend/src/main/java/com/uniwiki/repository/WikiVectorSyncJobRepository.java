package com.uniwiki.repository;

import com.uniwiki.entity.VectorSyncStatus;
import com.uniwiki.entity.WikiVectorSyncJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface WikiVectorSyncJobRepository
        extends JpaRepository<WikiVectorSyncJob, Long> {

    List<WikiVectorSyncJob>
    findTop50ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            Collection<VectorSyncStatus> statuses,
            int maxAttempts
    );

    List<WikiVectorSyncJob> findTop20ByOrderByCreatedAtDesc();

    long countByStatus(VectorSyncStatus status);

    Optional<WikiVectorSyncJob> findFirstByWikiPostIdAndStatusInOrderByIdDesc(
            Long wikiPostId,
            Collection<VectorSyncStatus> statuses);

    @Modifying
    @Query(value = """
            DELETE FROM wiki_vector_sync_jobs
            WHERE id IN (
                SELECT id FROM (
                    SELECT older.id
                    FROM wiki_vector_sync_jobs older
                    JOIN wiki_vector_sync_jobs newer
                      ON newer.wiki_post_id = older.wiki_post_id AND newer.id > older.id
                    WHERE older.status = 'COMPLETED'
                    LIMIT 50
                ) superseded
            )
            """, nativeQuery = true)
    int deleteSupersededCompletedJobs();

    @Modifying
    @Query(value = """
            UPDATE wiki_vector_sync_jobs
            SET payload = NULL
            WHERE status = 'COMPLETED' AND payload IS NOT NULL
            LIMIT 5
            """, nativeQuery = true)
    int clearCompletedPayloads();
}
