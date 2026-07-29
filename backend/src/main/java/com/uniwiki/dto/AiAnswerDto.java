package com.uniwiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AiAnswerDto {

    private AiAnswerDto() {
    }

    public record Request(
            @NotBlank @Size(max = 1000) String question,
            @Positive Long categoryId
    ) {
    }

    public record Source(Long wikiPostId, String title, String url) {
    }

    public record Response(
            String question,
            String answer,
            boolean grounded,
            int retrievedChunkCount,
            List<Source> sources
    ) {
    }
}
