package com.uniwiki.dto;

import java.util.List;

public record EverytimeLectureBatchResponseDto(
        int targetCount,
        int matchedCount,
        int savedReviewCount,
        int duplicateReviewCount,
        List<Item> items
) {
    public record Item(
            String courseName,
            String professor,
            String status,
            String lectureUrl,
            int savedReviewCount,
            int duplicateReviewCount,
            String message
    ) {
    }
}
