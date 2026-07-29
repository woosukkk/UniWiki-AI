package com.uniwiki.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminDashboardDto(
        long userCount,
        long wikiPostCount,
        long questionCount,
        long answerCount,
        long pendingSyncCount,
        long failedSyncCount,
        List<SyncJob> recentSyncJobs
) {
    public record SyncJob(
            Long id,
            Long wikiPostId,
            String operation,
            String status,
            int attemptCount,
            String lastError,
            LocalDateTime createdAt,
            LocalDateTime processedAt
    ) {
    }
}
