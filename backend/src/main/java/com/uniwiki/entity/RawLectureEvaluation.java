package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "raw_lecture_evaluations",
        indexes = {
                @Index(name = "idx_raw_lecture_processing", columnList = "is_processed,id"),
                @Index(name = "idx_raw_lecture_course_professor", columnList = "course_name,professor")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawLectureEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String sourceUrl; // 수집 출처 URL

    @Column(nullable = false, length = 200)
    private String courseName; // 강의명

    @Column(nullable = false, length = 100)
    private String professor; // 교수명

    @Column(nullable = false)
    private int starRating; // 별점 (1~5)

    @Column(nullable = false)
    private int likesCount; // 좋아요/추천 수

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 리뷰 내용

    @Column(nullable = false)
    private boolean isProcessed = false; // AI 학습용 정제 여부

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 20)
    private LectureReviewProcessingStatus processingStatus;

    @Lob
    @Column(name = "sanitized_content", columnDefinition = "TEXT")
    private String sanitizedContent;

    @Column(name = "processing_note", length = 500)
    private String processingNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "crawled_at", nullable = false, updatable = false)
    private LocalDateTime crawledAt; // 수집 일자

    public RawLectureEvaluation(String sourceUrl, String courseName, String professor, int starRating, int likesCount, String content) {
        this.sourceUrl = sourceUrl;
        this.courseName = courseName;
        this.professor = professor;
        this.starRating = starRating;
        this.likesCount = likesCount;
        this.content = content;
    }

    @PrePersist
    private void prePersist() {
        this.crawledAt = LocalDateTime.now();
        if (this.processingStatus == null) {
            this.processingStatus = LectureReviewProcessingStatus.PENDING;
        }
    }

    public void accept(String sanitizedContent) {
        this.sanitizedContent = sanitizedContent;
        this.processingStatus = LectureReviewProcessingStatus.ACCEPTED;
        this.processingNote = null;
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.sanitizedContent = null;
        this.processingStatus = LectureReviewProcessingStatus.REJECTED;
        this.processingNote = reason;
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
    }
}
