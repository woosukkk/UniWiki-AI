package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_evaluations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자 봇
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 200)
    private String courseName; // 강의명

    @Column(nullable = false, length = 100)
    private String professor; // 교수명

    @Column(nullable = false)
    private int starRating; // 별점 (1~5)

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 리뷰 내용

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CourseEvaluation(User author, String courseName, String professor, int starRating, String content) {
        this.author = author;
        this.courseName = courseName;
        this.professor = professor;
        this.starRating = starRating;
        this.content = content;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
