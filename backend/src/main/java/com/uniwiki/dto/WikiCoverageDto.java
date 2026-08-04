package com.uniwiki.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WikiCoverageDto(
        LocalDateTime generatedAt,
        LocalDateTime latestUpdatedAt,
        long totalRecords,
        long everytimeRecords,
        long wikiRecords,
        List<CategoryCoverage> categories,
        List<ContentTypeCoverage> everytimeContentTypes
) {
    public record CategoryCoverage(
            Long id,
            String name,
            String description,
            long count,
            String level
    ) { }

    public record ContentTypeCoverage(
            String type,
            long count
    ) { }
}
