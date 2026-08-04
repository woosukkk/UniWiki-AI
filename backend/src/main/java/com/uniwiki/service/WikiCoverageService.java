package com.uniwiki.service;

import com.uniwiki.dto.WikiCoverageDto;
import com.uniwiki.entity.EverytimeContentType;
import com.uniwiki.entity.WikiPostStatus;
import com.uniwiki.repository.EverytimeWikiDocumentRepository;
import com.uniwiki.repository.WikiPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiCoverageService {

    private final WikiPostRepository wikiPostRepository;
    private final EverytimeWikiDocumentRepository everytimeWikiDocumentRepository;

    public WikiCoverageDto getCoverage() {
        long totalRecords = wikiPostRepository.countByStatus(WikiPostStatus.APPROVED);
        List<WikiCoverageDto.CategoryCoverage> categories = wikiPostRepository
                .summarizeCategoriesByStatus(WikiPostStatus.APPROVED)
                .stream()
                .map(row -> new WikiCoverageDto.CategoryCoverage(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        level(((Number) row[3]).longValue())
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
        long everytimeRecords = everytimeContentTypes.stream()
                .mapToLong(WikiCoverageDto.ContentTypeCoverage::count)
                .sum();

        return new WikiCoverageDto(
                LocalDateTime.now(),
                wikiPostRepository.findLatestUpdatedAtByStatus(WikiPostStatus.APPROVED),
                totalRecords,
                everytimeRecords,
                Math.max(0, totalRecords - everytimeRecords),
                categories,
                everytimeContentTypes
        );
    }

    private String level(long count) {
        if (count < 10) return "scarce";
        if (count < 30) return "balanced";
        return "dense";
    }
}
