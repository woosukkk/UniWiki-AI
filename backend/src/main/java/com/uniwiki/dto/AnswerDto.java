package com.uniwiki.dto;

import com.uniwiki.entity.Answer;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AnswerDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "답변 내용은 필수입니다.")
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {

        @NotBlank(message = "답변 내용은 필수입니다.")
        private String content;
    }

    @Getter
    public static class Response {

        private final Long id;
        private final Long questionId;
        private final Long authorId;
        private final String authorNickname;
        private final String content;
        private final boolean accepted;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;

        private Response(
                Long id,
                Long questionId,
                Long authorId,
                String authorNickname,
                String content,
                boolean accepted,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {
            this.id = id;
            this.questionId = questionId;
            this.authorId = authorId;
            this.authorNickname = authorNickname;
            this.content = content;
            this.accepted = accepted;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public static Response from(Answer answer) {
            return new Response(
                    answer.getId(),
                    answer.getQuestion().getId(),
                    answer.getAuthor().getId(),
                    answer.getAuthor().getNickname(),
                    answer.getContent(),
                    answer.isAccepted(),
                    answer.getCreatedAt(),
                    answer.getUpdatedAt()
            );
        }
    }
}
