package com.uniwiki.dto;

public final class AiSummaryDto {
    private AiSummaryDto() {
    }

    public record Response(Long wikiPostId, String title, String summary, int sourceChunkCount) {
    }
}
