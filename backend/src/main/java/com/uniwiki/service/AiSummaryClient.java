package com.uniwiki.service;

import com.uniwiki.dto.AiSummaryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class AiSummaryClient {
    private final RestClient restClient;

    public AiSummaryClient(RestClient.Builder builder,
                           @Value("${uniwiki.ai-service.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public AiSummaryDto.Response summarize(Long wikiPostId, Integer maxChars) {
        return restClient.post()
                .uri(builder -> builder.path("/api/summaries/wiki-posts/{wikiPostId}")
                        .queryParamIfPresent("maxChars", Optional.ofNullable(maxChars))
                        .build(wikiPostId))
                .retrieve()
                .body(AiSummaryDto.Response.class);
    }
}
