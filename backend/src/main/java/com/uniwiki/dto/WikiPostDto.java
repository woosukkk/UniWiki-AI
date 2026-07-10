package com.uniwiki.dto;

import com.uniwiki.entity.WikiPost;
import com.uniwiki.entity.WikiPostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class WikiPostDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {

        @NotNull(message = "카테고리 ID는 필수입니다.")
        private Long categoryId;

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        @Size(max = 500, message = "요약은 500자 이하여야 합니다.")
        private String summary;

        @NotNull(message = "문서 상태는 필수입니다.")
        private WikiPostStatus status;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {

        @NotNull(message = "카테고리 ID는 필수입니다.")
        private Long categoryId;

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        @Size(max = 500, message = "요약은 500자 이하여야 합니다.")
        private String summary;

        @NotNull(message = "문서 상태는 필수입니다.")
        private WikiPostStatus status;
    }

    // 단건 조회용 응답
    @Getter
    public static class Response {

        private final Long id;

        private final Long categoryId;
        private final String categoryName;

        private final Long authorId;
        private final String authorNickname;

        private final String title;
        private final String content;
        private final String summary;

        private final WikiPostStatus status;
        private final Long viewCount;

        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;

        public Response(WikiPost wikiPost) {
            this.id = wikiPost.getId();

            this.categoryId = wikiPost.getCategory().getId();
            this.categoryName = wikiPost.getCategory().getName();

            this.authorId = wikiPost.getAuthor().getId();
            this.authorNickname = wikiPost.getAuthor().getNickname();

            this.title = wikiPost.getTitle();
            this.content = wikiPost.getContent();
            this.summary = wikiPost.getSummary();

            this.status = wikiPost.getStatus();
            this.viewCount = wikiPost.getViewCount();

            this.createdAt = wikiPost.getCreatedAt();
            this.updatedAt = wikiPost.getUpdatedAt();
        }
    }

    // 목록 조회용 응답
    @Getter
    public static class ListResponse {

        private final Long id;

        private final Long categoryId;
        private final String categoryName;

        private final Long authorId;
        private final String authorNickname;

        private final String title;
        private final String summary;

        private final WikiPostStatus status;
        private final Long viewCount;

        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;

        public ListResponse(WikiPost wikiPost) {
            this.id = wikiPost.getId();

            this.categoryId = wikiPost.getCategory().getId();
            this.categoryName = wikiPost.getCategory().getName();

            this.authorId = wikiPost.getAuthor().getId();
            this.authorNickname = wikiPost.getAuthor().getNickname();

            this.title = wikiPost.getTitle();
            this.summary = wikiPost.getSummary();

            this.status = wikiPost.getStatus();
            this.viewCount = wikiPost.getViewCount();

            this.createdAt = wikiPost.getCreatedAt();
            this.updatedAt = wikiPost.getUpdatedAt();
        }
    }
}