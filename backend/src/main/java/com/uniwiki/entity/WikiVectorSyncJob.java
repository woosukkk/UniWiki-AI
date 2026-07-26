package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "wiki_vector_sync_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WikiVectorSyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wiki_post_id", nullable = false)
    private Long wikiPostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VectorSyncOperation operation;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VectorSyncStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private WikiVectorSyncJob(
            Long wikiPostId,
            VectorSyncOperation operation,
            String payload
    ) {
        this.wikiPostId = wikiPostId;
        this.operation = operation;
        this.payload = payload;
        this.status = VectorSyncStatus.PENDING;
        this.attemptCount = 0;
    }

    public static WikiVectorSyncJob upsert(Long wikiPostId, String payload) {
        return new WikiVectorSyncJob(wikiPostId, VectorSyncOperation.UPSERT, payload);
    }

    public static WikiVectorSyncJob delete(Long wikiPostId) {
        return new WikiVectorSyncJob(wikiPostId, VectorSyncOperation.DELETE, null);
    }

    public void markCompleted() {
        this.status = VectorSyncStatus.COMPLETED;
        this.attemptCount++;
        this.lastError = null;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.status = VectorSyncStatus.FAILED;
        this.attemptCount++;
        this.lastError = truncate(error, 2000);
        this.processedAt = null;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
