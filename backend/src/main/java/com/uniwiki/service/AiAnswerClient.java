package com.uniwiki.service;

import com.uniwiki.dto.AiAnswerDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        try {
            return restClient.post()
                    .uri("/api/rag/answers")
                    .body(request)
                    .retrieve()
                    .body(AiAnswerDto.Response.class);
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    exception.getStatusCode(),
                    "AI 서비스가 요청을 처리하지 못했습니다.",
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서비스에 연결할 수 없습니다.",
                    exception
            );
        }
    }
}
