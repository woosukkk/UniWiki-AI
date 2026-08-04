package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_community_posts", indexes = {
        @Index(name = "idx_raw_community_processing", columnList = "is_processed,id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawCommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String sourceUrl; // 수집 출처 URL

    @Column(nullable = false, length = 100)
    private String boardType; // 게시판 종류 (ex: 자유게시판)

    @Column(nullable = false, length = 500)
    private String title; // 글 제목

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 글 본문

    @Column(nullable = false)
    private int likesCount; // 좋아요 수

    @Column(nullable = false)
    private int commentsCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String commentsJson; // 댓글 목록 JSON

    @Column(nullable = false)
    private boolean isProcessed = false; // AI 학습용 정제 여부

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private CommunityPostProcessingStatus processingStatus = CommunityPostProcessingStatus.PENDING;

    @Column(name = "usefulness_score")
    private Integer usefulnessScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 30)
    private EverytimeContentType contentType;

    @Lob
    @Column(name = "sanitized_content", columnDefinition = "TEXT")
    private String sanitizedContent;

    @Column(name = "processing_note", length = 500)
    private String processingNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "crawled_at", nullable = false, updatable = false)
    private LocalDateTime crawledAt; // 수집 일자

    public RawCommunityPost(String sourceUrl, String boardType, String title, String content,
                            int likesCount, int commentsCount, String commentsJson) {
        this.sourceUrl = sourceUrl;
        this.boardType = boardType;
        this.title = title;
        this.content = content;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.commentsJson = commentsJson;
    }

    @PrePersist
    private void prePersist() {
        this.crawledAt = LocalDateTime.now();
    }

    public void accept(int score, EverytimeContentType contentType, String sanitizedContent) {
        this.usefulnessScore = score;
        this.contentType = contentType;
        this.sanitizedContent = sanitizedContent;
        this.processingStatus = CommunityPostProcessingStatus.ACCEPTED;
        this.processingNote = null;
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(int score, String reason) {
        this.usefulnessScore = score;
        this.processingStatus = CommunityPostProcessingStatus.REJECTED;
        this.processingNote = reason;
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
    }
}
