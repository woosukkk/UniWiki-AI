package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "official_attachments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_official_attachment_url", columnNames = {"raw_document_id", "source_url"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_document_id", nullable = false)
    private RawOfficialDocument rawDocument;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 20)
    private AttachmentExtractionStatus extractionStatus;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "MEDIUMTEXT")
    private String extractedText;

    @Column(name = "extraction_error", length = 1000)
    private String extractionError;

    @Column(name = "first_collected_at", nullable = false, updatable = false)
    private LocalDateTime firstCollectedAt;

    @Column(name = "last_collected_at", nullable = false)
    private LocalDateTime lastCollectedAt;

    public OfficialAttachment(RawOfficialDocument rawDocument, String sourceUrl, String fileName,
                              String contentType, long fileSize, String contentHash,
                              AttachmentExtractionStatus extractionStatus, String extractedText,
                              String extractionError) {
        this.rawDocument = rawDocument;
        update(sourceUrl, fileName, contentType, fileSize, contentHash,
                extractionStatus, extractedText, extractionError);
    }

    public void update(String sourceUrl, String fileName, String contentType, long fileSize,
                       String contentHash, AttachmentExtractionStatus extractionStatus,
                       String extractedText, String extractionError) {
        this.sourceUrl = sourceUrl;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.contentHash = contentHash;
        this.extractionStatus = extractionStatus;
        this.extractedText = extractedText;
        this.extractionError = truncate(extractionError, 1000);
        this.lastCollectedAt = LocalDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        firstCollectedAt = now;
        lastCollectedAt = now;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
