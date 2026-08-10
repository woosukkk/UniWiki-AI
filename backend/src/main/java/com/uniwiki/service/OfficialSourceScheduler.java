package com.uniwiki.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "uniwiki.official-sources.scheduler-enabled", havingValue = "true")
public class OfficialSourceScheduler {
    private final OfficialSourcePipelineService pipelineService;

    @Scheduled(
            cron = "${uniwiki.official-sources.cron:0 0 0 * * *}",
            zone = "${uniwiki.official-sources.zone:Asia/Seoul}"
    )
    public void collect() {
        pipelineService.collectActiveSources();
    }
}
