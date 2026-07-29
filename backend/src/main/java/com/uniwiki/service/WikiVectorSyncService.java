package com.uniwiki.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwiki.dto.WikiVectorSyncPayload;
import com.uniwiki.entity.*;
import com.uniwiki.repository.WikiVectorSyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WikiVectorSyncService {

    private static final List<VectorSyncStatus> RETRYABLE_STATUSES = List.of(
            VectorSyncStatus.PENDING,
            VectorSyncStatus.FAILED
    );

    private final WikiVectorSyncJobRepository jobRepository;
    private final AiVectorStoreClient aiVectorStoreClient;
    private final ObjectMapper objectMapper;

    @Value("${uniwiki.vector-sync.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public void enqueueUpsert(WikiPost wikiPost) {
        String payload = serialize(WikiVectorSyncPayload.from(wikiPost));
        jobRepository.save(WikiVectorSyncJob.upsert(wikiPost.getId(), payload));
    }

    @Transactional
    public void enqueueDelete(Long wikiPostId) {
        jobRepository.save(WikiVectorSyncJob.delete(wikiPostId));
    }

    @Transactional
    public int processPendingJobs() {
        List<WikiVectorSyncJob> jobs = jobRepository
                .findTop50ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                        RETRYABLE_STATUSES,
                        maxAttempts
                );

        for (WikiVectorSyncJob job : jobs) {
            process(job);
        }
        return jobs.size();
    }

    private void process(WikiVectorSyncJob job) {
        try {
            if (job.getOperation() == VectorSyncOperation.UPSERT) {
                aiVectorStoreClient.upsert(deserialize(job.getPayload()));
            } else {
                aiVectorStoreClient.delete(job.getWikiPostId());
            }
            job.markCompleted();
        } catch (Exception exception) {
            job.markFailed(errorMessage(exception));
        }
    }

    private String serialize(WikiVectorSyncPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("벡터 동기화 데이터 생성에 실패했습니다.", exception);
        }
    }

    private WikiVectorSyncPayload deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, WikiVectorSyncPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("벡터 동기화 데이터를 읽을 수 없습니다.", exception);
        }
    }

    private String errorMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
