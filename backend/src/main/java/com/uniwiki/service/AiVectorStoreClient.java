package com.uniwiki.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwiki.dto.WikiVectorSyncPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class AiVectorStoreClient {

    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public AiVectorStoreClient(
            ObjectMapper objectMapper,
            @Value("${uniwiki.ai-service.base-url:http://localhost:8000}") String baseUrl
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public void upsert(WikiVectorSyncPayload payload) {
        byte[] json = toJson(payload).getBytes(StandardCharsets.UTF_8);
        send("PUT", "/api/vector-store/wiki-posts", json, 180_000);
    }

    public void delete(Long wikiPostId) {
        send("DELETE", "/api/vector-store/wiki-posts/" + wikiPostId, null, 30_000);
    }

    private String toJson(WikiVectorSyncPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 벡터 요청을 JSON으로 변환할 수 없습니다.", exception);
        }
    }

    private void send(String method, String path, byte[] body, int readTimeoutMillis) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(readTimeoutMillis);
            connection.setRequestProperty("Accept", "application/json");
            if (body != null) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.getOutputStream().write(body);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String responseBody = connection.getErrorStream() == null
                        ? ""
                        : new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException(
                        "AI 벡터 요청 실패: HTTP " + status + " " + responseBody);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "AI 벡터 서비스에 연결할 수 없습니다: " + exception.getMessage(), exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
