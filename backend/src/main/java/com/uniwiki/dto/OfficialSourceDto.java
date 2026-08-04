package com.uniwiki.dto;

import com.uniwiki.entity.OfficialSource;
import com.uniwiki.entity.RawOfficialDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class OfficialSourceDto {
    private OfficialSourceDto() { }

    public record CreateRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 1000) String listUrl,
            @NotBlank @Size(max = 500) String articleLinkSelector,
            @NotBlank @Size(max = 500) String titleSelector,
            @NotBlank @Size(max = 500) String contentSelector,
            boolean autoPublish
    ) { }

    public record Response(
            Long id,
            Long categoryId,
            String categoryName,
            String name,
            String listUrl,
            boolean autoPublish,
            boolean active,
            LocalDateTime lastCheckedAt,
            LocalDateTime lastSuccessAt,
            String lastError
    ) {
        public static Response from(OfficialSource source) {
            return new Response(
                    source.getId(), source.getCategory().getId(), source.getCategory().getName(),
                    source.getName(), source.getListUrl(), source.isAutoPublish(), source.isActive(),
                    source.getLastCheckedAt(), source.getLastSuccessAt(), source.getLastError());
        }
    }

    public record CollectionResult(int discovered, int created, int changed, int unchanged, int failed) { }

    public record DocumentResponse(
            Long id,
            Long sourceId,
            String sourceName,
            String sourceUrl,
            String title,
            String contentHash,
            String status,
            LocalDateTime firstCollectedAt,
            LocalDateTime lastCollectedAt,
            LocalDateTime changedAt
    ) {
        public static DocumentResponse from(RawOfficialDocument document) {
            return new DocumentResponse(
                    document.getId(), document.getOfficialSource().getId(),
                    document.getOfficialSource().getName(), document.getSourceUrl(),
                    document.getTitle(), document.getContentHash(),
                    document.getProcessingStatus().name(), document.getFirstCollectedAt(),
                    document.getLastCollectedAt(), document.getChangedAt());
        }
    }
}
