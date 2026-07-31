package com.uniwiki.service;

import com.uniwiki.dto.AiAnswerDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class AiAnswerClient {

    private final RestClient restClient;

    public AiAnswerClient(
            RestClient.Builder builder,
            @Value("${uniwiki.ai-service.base-url:http://localhost:8000}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(180_000);
        this.restClient = builder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();
    }

    public AiAnswerDto.Response answer(AiAnswerDto.Request request) {
        return restClient.post()
                .uri("/api/rag/answers")
                .body(request)
                .retrieve()
                .body(AiAnswerDto.Response.class);
    }
}
