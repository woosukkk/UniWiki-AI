package com.uniwiki.dto;

import com.uniwiki.entity.Question;
import com.uniwiki.entity.QuestionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class QuestionDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {

        // 제목은 공백일 수 없고 최대 255자까지 허용
        @NotBlank(message = "질문 제목은 필수입니다.")
        @Size(max = 255, message = "질문 제목은 255자 이하여야 합니다.")
        private String title;

        // 질문 내용은 필수
        @NotBlank(message = "질문 내용은 필수입니다.")
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {

        @NotBlank(message = "질문 제목은 필수입니다.")
        @Size(max = 255, message = "질문 제목은 255자 이하여야 합니다.")
        private String title;

        @NotBlank(message = "질문 내용은 필수입니다.")
        private String content;
    }

    @Getter
    public static class Response {

        private final Long id;
        private final Long authorId;
        private final String authorNickname;
        private final String title;
        private final String content;
        private final QuestionStatus status;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;

        private Response(
                Long id,
                Long authorId,
                String authorNickname,
                String title,
                String content,
                QuestionStatus status,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {
            this.id = id;
            this.authorId = authorId;
            this.authorNickname = authorNickname;
            this.title = title;
            this.content = content;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        // Question 엔티티를 API 응답 DTO로 변환
        public static Response from(Question question) {
            return new Response(
                    question.getId(),
                    question.getAuthor().getId(),
                    question.getAuthor().getNickname(),
                    question.getTitle(),
                    question.getContent(),
                    question.getStatus(),
                    question.getCreatedAt(),
                    question.getUpdatedAt()
            );
        }
    }
}