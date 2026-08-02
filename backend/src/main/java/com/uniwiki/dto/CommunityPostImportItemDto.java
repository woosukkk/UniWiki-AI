package com.uniwiki.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityPostImportItemDto(
        @NotBlank @Size(max = 1000) String sourceUrl,
        @NotBlank @Size(max = 100) String boardType,
        @NotBlank @Size(max = 500) String title,
        @NotBlank String content,
        @Min(0) int likesCount,
        @Min(0) int commentsCount,
        String commentsJson
) { }
