package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_official_documents", uniqueConstraints = {
        @UniqueConstraint(name = "uq_raw_official_source_url", columnNames = {"official_source_id", "source_url"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawOfficialDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "official_source_id", nullable = false)
    private OfficialSource officialSource;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private OfficialDocumentStatus processingStatus;

    @Column(name = "first_collected_at", nullable = false, updatable = false)
    private LocalDateTime firstCollectedAt;

    @Column(name = "last_collected_at", nullable = false)
    private LocalDateTime lastCollectedAt;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public RawOfficialDocument(OfficialSource officialSource, String sourceUrl,
                               String title, String content, String contentHash) {
        this.officialSource = officialSource;
        this.sourceUrl = sourceUrl;
        this.title = title;
        this.content = content;
        this.contentHash = contentHash;
        this.processingStatus = OfficialDocumentStatus.PENDING;
    }

    public boolean updateIfChanged(String title, String content, String newHash) {
        this.lastCollectedAt = LocalDateTime.now();
        if (contentHash.equals(newHash)) return false;
        this.title = title;
        this.content = content;
        this.contentHash = newHash;
        this.processingStatus = OfficialDocumentStatus.PENDING;
        this.changedAt = LocalDateTime.now();
        return true;
    }

    public void markProcessed(boolean published) {
        this.processingStatus = published ? OfficialDocumentStatus.PUBLISHED : OfficialDocumentStatus.DRAFTED;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        firstCollectedAt = now;
        lastCollectedAt = now;
        changedAt = now;
    }
}
