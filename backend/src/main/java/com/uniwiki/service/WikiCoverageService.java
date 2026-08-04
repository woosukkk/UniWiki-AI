package com.uniwiki.service;

import com.uniwiki.dto.WikiCoverageDto;
import com.uniwiki.entity.*;
import com.uniwiki.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiCoverageService {

    private final WikiPostRepository wikiPostRepository;
    private final EverytimeWikiDocumentRepository everytimeWikiDocumentRepository;
    private final OfficialWikiDocumentRepository officialWikiDocumentRepository;
    private final OfficialAttachmentRepository officialAttachmentRepository;
    private final WikiVectorSyncJobRepository vectorSyncJobRepository;

    public WikiCoverageDto getCoverage() {
        List<WikiPost> approvedPosts = wikiPostRepository
                .findAllByStatusOrderByCreatedAtDesc(WikiPostStatus.APPROVED);
        Set<Long> officialPostIds = postIds(officialWikiDocumentRepository.findApprovedWikiPostCategoryPairs());
        Set<Long> communityPostIds = postIds(everytimeWikiDocumentRepository.findApprovedWikiPostCategoryPairs());
        Map<Long, Long> officialByCategory = countsByCategory(
                officialWikiDocumentRepository.findApprovedWikiPostCategoryPairs());
        Map<Long, Long> communityByCategory = countsByCategory(
                everytimeWikiDocumentRepository.findApprovedWikiPostCategoryPairs());
        Map<Long, AttachmentStats> attachmentsByCategory = attachmentStats();
        Set<Long> vectorSyncedPostIds = latestVectorStatuses().entrySet().stream()
                .filter(entry -> entry.getValue() == VectorSyncStatus.COMPLETED)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<WikiCoverageDto.CategoryCoverage> categories = wikiPostRepository
                .summarizeCategoriesByStatus(WikiPostStatus.APPROVED)
                .stream()
                .map(row -> categoryCoverage(
                        row,
                        officialByCategory,
                        communityByCategory,
                        attachmentsByCategory,
                        approvedPosts,
                        vectorSyncedPostIds
                ))
                .toList();

        List<WikiCoverageDto.ContentTypeCoverage> everytimeContentTypes =
                everytimeWikiDocumentRepository.summarizeApprovedByContentType()
                        .stream()
                        .map(row -> new WikiCoverageDto.ContentTypeCoverage(
                                ((EverytimeContentType) row[0]).name(),
                                ((Number) row[1]).longValue()
                        ))
                        .toList();

        long totalRecords = approvedPosts.size();
        long officialRecords = officialPostIds.size();
        long everytimeRecords = communityPostIds.size();
        long wikiRecords = Math.max(0, totalRecords - everytimeRecords);
        long attachmentRecords = attachmentsByCategory.values().stream()
                .mapToLong(AttachmentStats::total).sum();
        long extractedAttachmentRecords = attachmentsByCategory.values().stream()
                .mapToLong(AttachmentStats::extracted).sum();
        long vectorSyncedRecords = approvedPosts.stream()
                .map(WikiPost::getId)
                .filter(vectorSyncedPostIds::contains)
                .count();
        int trustScore = weightedTrustScore(categories);

        return new WikiCoverageDto(
                LocalDateTime.now(),
                wikiPostRepository.findLatestUpdatedAtByStatus(WikiPostStatus.APPROVED),
                totalRecords,
                everytimeRecords,
                wikiRecords,
                officialRecords,
                attachmentRecords,
                extractedAttachmentRecords,
                vectorSyncedRecords,
                trustScore,
                categories,
                everytimeContentTypes
        );
    }

    private WikiCoverageDto.CategoryCoverage categoryCoverage(
            Object[] row,
            Map<Long, Long> officialByCategory,
            Map<Long, Long> communityByCategory,
            Map<Long, AttachmentStats> attachmentsByCategory,
            List<WikiPost> approvedPosts,
            Set<Long> vectorSyncedPostIds
    ) {
        Long categoryId = (Long) row[0];
        long count = ((Number) row[3]).longValue();
        LocalDateTime latestUpdatedAt = (LocalDateTime) row[4];
        long officialCount = officialByCategory.getOrDefault(categoryId, 0L);
        long communityCount = communityByCategory.getOrDefault(categoryId, 0L);
        long otherCount = Math.max(0, count - officialCount - communityCount);
        AttachmentStats attachmentStats = attachmentsByCategory.getOrDefault(categoryId, AttachmentStats.EMPTY);

        List<WikiPost> categoryPosts = approvedPosts.stream()
                .filter(post -> post.getCategory().getId().equals(categoryId))
                .toList();
        long synced = categoryPosts.stream().map(WikiPost::getId).filter(vectorSyncedPostIds::contains).count();

        int sourceScore = percentage(
                officialCount * 100 + otherCount * 70 + communityCount * 45,
                count * 100
        );
        int freshnessScore = freshnessScore(latestUpdatedAt);
        int attachmentScore = attachmentScore(officialCount, attachmentStats);
        int vectorScore = percentage(synced, count);
        int trustScore = weightedScore(sourceScore, freshnessScore, attachmentScore, vectorScore);

        return new WikiCoverageDto.CategoryCoverage(
                categoryId,
                (String) row[1],
                (String) row[2],
                count,
                volumeLevel(count),
                officialCount,
                communityCount,
                otherCount,
                trustScore,
                trustLevel(trustScore),
                sourceScore,
                freshnessScore,
                attachmentScore,
                vectorScore,
                latestUpdatedAt
        );
    }

    private Set<Long> postIds(List<Object[]> pairs) {
        return pairs.stream().map(row -> (Long) row[0]).collect(Collectors.toSet());
    }

    private Map<Long, Long> countsByCategory(List<Object[]> pairs) {
        return pairs.stream().collect(Collectors.groupingBy(row -> (Long) row[1], Collectors.counting()));
    }

    private Map<Long, AttachmentStats> attachmentStats() {
        Map<Long, AttachmentStats> stats = new HashMap<>();
        for (Object[] row : officialAttachmentRepository.summarizePublishedByCategoryAndStatus()) {
            Long categoryId = (Long) row[0];
            AttachmentExtractionStatus status = (AttachmentExtractionStatus) row[1];
            long count = ((Number) row[2]).longValue();
            stats.merge(categoryId, AttachmentStats.of(status, count), AttachmentStats::add);
        }
        return stats;
    }

    private Map<Long, VectorSyncStatus> latestVectorStatuses() {
        return vectorSyncJobRepository.findAll().stream()
                .sorted(Comparator.comparing(WikiVectorSyncJob::getUpdatedAt).reversed())
                .collect(Collectors.toMap(
                        WikiVectorSyncJob::getWikiPostId,
                        WikiVectorSyncJob::getStatus,
                        (latest, ignored) -> latest,
                        LinkedHashMap::new
                ));
    }

    private int freshnessScore(LocalDateTime updatedAt) {
        if (updatedAt == null) return 0;
        long days = Math.max(0, Duration.between(updatedAt, LocalDateTime.now()).toDays());
        if (days <= 30) return 100;
        if (days <= 90) return 85;
        if (days <= 180) return 70;
        if (days <= 365) return 50;
        return 25;
    }

    private int attachmentScore(long officialCount, AttachmentStats stats) {
        if (officialCount == 0) return 70;
        if (stats.total() == 0) return 85;
        long points = stats.extracted() * 100 + stats.empty() * 60 + stats.unsupported() * 35;
        return percentage(points, stats.total() * 100);
    }

    private int weightedScore(int source, int freshness, int attachment, int vector) {
        return (int) Math.round(source * 0.45 + freshness * 0.25 + attachment * 0.15 + vector * 0.15);
    }

    private int weightedTrustScore(List<WikiCoverageDto.CategoryCoverage> categories) {
        long total = categories.stream().mapToLong(WikiCoverageDto.CategoryCoverage::count).sum();
        if (total == 0) return 0;
        double weighted = categories.stream()
                .mapToDouble(category -> category.trustScore() * category.count())
                .sum();
        return (int) Math.round(weighted / total);
    }

    private int percentage(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return (int) Math.max(0, Math.min(100, Math.round(numerator * 100.0 / denominator)));
    }

    private String volumeLevel(long count) {
        if (count < 10) return "scarce";
        if (count < 30) return "balanced";
        return "dense";
    }

    private String trustLevel(int score) {
        if (score < 55) return "low";
        if (score < 75) return "medium";
        return "high";
    }

    private record AttachmentStats(long total, long extracted, long empty, long unsupported) {
        private static final AttachmentStats EMPTY = new AttachmentStats(0, 0, 0, 0);

        private static AttachmentStats of(AttachmentExtractionStatus status, long count) {
            return switch (status) {
                case EXTRACTED -> new AttachmentStats(count, count, 0, 0);
                case EMPTY -> new AttachmentStats(count, 0, count, 0);
                case UNSUPPORTED -> new AttachmentStats(count, 0, 0, count);
                case FAILED, TOO_LARGE -> new AttachmentStats(count, 0, 0, 0);
            };
        }

        private AttachmentStats add(AttachmentStats other) {
            return new AttachmentStats(
                    total + other.total,
                    extracted + other.extracted,
                    empty + other.empty,
                    unsupported + other.unsupported
            );
        }
    }
}
