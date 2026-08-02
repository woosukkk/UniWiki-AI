package com.uniwiki.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LectureReviewImportItemDto(
        @NotBlank @Size(max = 1000) String sourceUrl,
        @NotBlank @Size(max = 200) String courseName,
        @NotBlank @Size(max = 100) String professor,
        @Min(0) @Max(5) int starRating,
        @Min(0) int likesCount,
        @NotBlank String content
) {
}
