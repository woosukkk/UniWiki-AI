package com.uniwiki.repository;

import com.uniwiki.entity.VectorSyncStatus;
import com.uniwiki.entity.WikiVectorSyncJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WikiVectorSyncJobRepository
        extends JpaRepository<WikiVectorSyncJob, Long> {

    List<WikiVectorSyncJob>
    findTop50ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            Collection<VectorSyncStatus> statuses,
            int maxAttempts
    );
}
