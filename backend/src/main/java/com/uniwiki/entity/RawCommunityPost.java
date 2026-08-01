package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_community_posts")
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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String commentsJson; // 댓글 목록 JSON

    @Column(nullable = false)
    private boolean isProcessed = false; // AI 학습용 정제 여부

    @Column(name = "crawled_at", nullable = false, updatable = false)
    private LocalDateTime crawledAt; // 수집 일자

    public RawCommunityPost(String sourceUrl, String boardType, String title, String content, int likesCount, String commentsJson) {
        this.sourceUrl = sourceUrl;
        this.boardType = boardType;
        this.title = title;
        this.content = content;
        this.likesCount = likesCount;
        this.commentsJson = commentsJson;
    }

    @PrePersist
    private void prePersist() {
        this.crawledAt = LocalDateTime.now();
    }
}
