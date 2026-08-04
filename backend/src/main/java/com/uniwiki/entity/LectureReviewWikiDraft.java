package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lecture_review_wiki_drafts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_lecture_review_course_professor",
                columnNames = {"course_name", "professor"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureReviewWikiDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @Column(nullable = false, length = 100)
    private String professor;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wiki_post_id", nullable = false, unique = true)
    private WikiPost wikiPost;

    @Column(name = "included_review_count", nullable = false)
    private int includedReviewCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LectureReviewWikiDraft(String courseName, String professor, WikiPost wikiPost, int includedReviewCount) {
        this.courseName = courseName;
        this.professor = professor;
        this.wikiPost = wikiPost;
        this.includedReviewCount = includedReviewCount;
    }

    public void refresh(int includedReviewCount) {
        this.includedReviewCount = includedReviewCount;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }
}
