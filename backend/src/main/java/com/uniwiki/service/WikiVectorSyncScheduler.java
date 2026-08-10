package com.uniwiki.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WikiVectorSyncScheduler {

    private final WikiVectorSyncService syncService;

    @Scheduled(fixedDelayString = "${uniwiki.vector-sync.interval-ms:60000}")
    public void synchronize() {
        syncService.processPendingJobs();
        syncService.pruneCompletedJobs();
    }
}
