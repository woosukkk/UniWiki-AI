package com.uniwiki.service;

import com.uniwiki.dto.WikiVectorSyncPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiVectorStoreClient {

    private final RestClient restClient;

    public AiVectorStoreClient(
            RestClient.Builder builder,
            @Value("${uniwiki.ai-service.base-url:http://localhost:8000}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public void upsert(WikiVectorSyncPayload payload) {
        restClient.put()
                .uri("/api/vector-store/wiki-posts")
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    public void delete(Long wikiPostId) {
        restClient.delete()
                .uri("/api/vector-store/wiki-posts/{wikiPostId}", wikiPostId)
                .retrieve()
                .toBodilessEntity();
    }
}
