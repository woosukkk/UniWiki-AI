package com.uniwiki.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(22)
@ConditionalOnProperty(
        name = "uniwiki.official-sources.topic-rebuild-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OfficialTopicWikiBootstrap implements ApplicationRunner {

    private final OfficialSourcePipelineService pipelineService;
    private final WikiVectorSyncService vectorSyncService;

    @Override
    public void run(ApplicationArguments args) {
        int pruned = vectorSyncService.pruneCompletedJobs();
        int rebuilt = pipelineService.rebuildTopicWikis();
        log.info("Official topic wiki rebuild completed: documents={}, prunedVectorJobs={}",
                rebuilt, pruned);
    }
}
