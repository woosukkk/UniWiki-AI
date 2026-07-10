package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wiki_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WikiPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 위키 문서 카테고리
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 위키 문서 작성자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WikiPostStatus status;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WikiPost(
            Category category,
            User author,
            String title,
            String content,
            String summary,
            WikiPostStatus status
    ) {
        this.category = category;
        this.author = author;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.status = status;
        this.viewCount = 0L;
    }

    // 위키 문서 수정
    public void update(
            Category category,
            String title,
            String content,
            String summary,
            WikiPostStatus status
    ) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.status = status;
    }

    // 조회수 증가
    public void increaseViewCount() {
        this.viewCount++;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.viewCount == null) {
            this.viewCount = 0L;
        }

        if (this.status == null) {
            this.status = WikiPostStatus.DRAFT;
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}