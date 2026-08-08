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
            fixedDelayString = "${uniwiki.official-sources.interval-ms:3600000}",
            initialDelayString = "${uniwiki.official-sources.initial-delay-ms:60000}"
    )
    public void collect() {
        pipelineService.collectActiveSources();
    }
}
