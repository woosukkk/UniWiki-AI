package com.uniwiki.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WikiCoverageDto(
        LocalDateTime generatedAt,
        LocalDateTime latestUpdatedAt,
        long totalRecords,
        long everytimeRecords,
        long wikiRecords,
        long officialRecords,
        long attachmentRecords,
        long extractedAttachmentRecords,
        long vectorSyncedRecords,
        int trustScore,
        List<CategoryCoverage> categories,
        List<ContentTypeCoverage> everytimeContentTypes
) {
    public record CategoryCoverage(
            Long id,
            String name,
            String description,
            long count,
            String level,
            long officialCount,
            long communityCount,
            long otherCount,
            int trustScore,
            String trustLevel,
            int sourceScore,
            int freshnessScore,
            int attachmentScore,
            int vectorScore,
            LocalDateTime latestUpdatedAt
    ) { }

    public record ContentTypeCoverage(
            String type,
            long count
    ) { }
}
