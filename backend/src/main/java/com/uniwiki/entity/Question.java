package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 질문 작성자와 User 엔티티를 연결
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Enum 값을 문자열 형태로 DB에 저장
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 질문 생성 시 작성자, 제목, 내용을 전달받음
    public Question(User author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.status = QuestionStatus.OPEN;
    }

    // 질문 수정
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // 질문 종료 처리
    public void close() {
        this.status = QuestionStatus.CLOSED;
    }

    // 종료된 질문을 다시 열 때 사용
    public void reopen() {
        this.status = QuestionStatus.OPEN;
    }

    // 현재 로그인 사용자가 질문 작성자인지 확인
    public boolean isAuthor(Long userId) {
        return author.getId().equals(userId);
    }

    // 엔티티가 처음 저장되기 직전에 실행
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = QuestionStatus.OPEN;
        }
    }

    // 엔티티가 수정되기 직전에 실행
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}