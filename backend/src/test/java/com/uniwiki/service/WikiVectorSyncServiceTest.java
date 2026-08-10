package com.uniwiki.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwiki.dto.WikiVectorSyncPayload;
import com.uniwiki.entity.*;
import com.uniwiki.repository.WikiVectorSyncJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiVectorSyncServiceTest {

    @Mock
    private WikiVectorSyncJobRepository jobRepository;

    @Mock
    private AiVectorStoreClient aiVectorStoreClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    private WikiVectorSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new WikiVectorSyncService(
                jobRepository,
                aiVectorStoreClient,
                new ObjectMapper(),
                transactionManager
        );
        ReflectionTestUtils.setField(syncService, "maxAttempts", 5);
    }

    @Test
    void enqueuesWikiUpsertWithSnapshotPayload() {
        WikiPost wikiPost = mock(WikiPost.class);
        Category category = mock(Category.class);
        when(wikiPost.getId()).thenReturn(7L);
        when(wikiPost.getTitle()).thenReturn("수강신청 안내");
        when(wikiPost.getContent()).thenReturn("포털에서 신청합니다.");
        when(wikiPost.getCategory()).thenReturn(category);
        when(category.getId()).thenReturn(2L);

        syncService.enqueueUpsert(wikiPost);

        ArgumentCaptor<WikiVectorSyncJob> captor =
                ArgumentCaptor.forClass(WikiVectorSyncJob.class);
        verify(jobRepository).save(captor.capture());
        WikiVectorSyncJob job = captor.getValue();
        assertThat(job.getWikiPostId()).isEqualTo(7L);
        assertThat(job.getOperation()).isEqualTo(VectorSyncOperation.UPSERT);
        assertThat(job.getStatus()).isEqualTo(VectorSyncStatus.PENDING);
        assertThat(job.getPayload()).contains("수강신청 안내", "포털에서 신청합니다.");
    }

    @Test
    void processesUpsertJobAndMarksItCompleted() {
        WikiVectorSyncJob job = WikiVectorSyncJob.upsert(
                7L,
                "{\"wikiPostId\":7,\"title\":\"제목\",\"content\":\"본문\",\"categoryId\":2}"
        );
        when(jobRepository
                .findTop50ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(any(), eq(5)))
                .thenReturn(List.of(job));

        int processed = syncService.processPendingJobs();

        assertThat(processed).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(VectorSyncStatus.COMPLETED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getProcessedAt()).isNotNull();
        assertThat(job.getPayload()).isNull();
        ArgumentCaptor<WikiVectorSyncPayload> payloadCaptor =
                ArgumentCaptor.forClass(WikiVectorSyncPayload.class);
        verify(aiVectorStoreClient).upsert(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().wikiPostId()).isEqualTo(7L);
    }

    @Test
    void processesDeleteJob() {
        WikiVectorSyncJob job = WikiVectorSyncJob.delete(9L);
        when(jobRepository
                .findTop50ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(any(), eq(5)))
                .thenReturn(List.of(job));

        syncService.processPendingJobs();

        verify(aiVectorStoreClient).delete(9L);
        assertThat(job.getStatus()).isEqualTo(VectorSyncStatus.COMPLETED);
    }

    @Test
    void recordsFailureForAutomaticRetry() {
        WikiVectorSyncJob job = WikiVectorSyncJob.delete(9L);
        when(jobRepository
                .findTop50ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(any(), eq(5)))
                .thenReturn(List.of(job));
        doThrow(new RuntimeException("AI service unavailable"))
                .when(aiVectorStoreClient).delete(9L);

        syncService.processPendingJobs();

        assertThat(job.getStatus()).isEqualTo(VectorSyncStatus.FAILED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getLastError()).isEqualTo("AI service unavailable");
        assertThat(job.getProcessedAt()).isNull();
    }
}
