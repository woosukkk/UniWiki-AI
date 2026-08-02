package com.uniwiki.service;

import com.uniwiki.dto.EverytimeLectureBatchRequestDto;
import com.uniwiki.dto.EverytimeLectureBatchResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "uniwiki.everytime.run-on-startup", havingValue = "true")
public class EverytimeLectureCollectorRunner implements ApplicationRunner {

    private final EverytimeLectureBatchService batchService;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${uniwiki.everytime.collector-terms:2026-2}")
    private String collectorTerms;

    @Value("${uniwiki.everytime.collector-max-pairs:1}")
    private int maxPairs;

    @Value("${uniwiki.everytime.collector-start-page:1}")
    private int startPage;

    @Value("${uniwiki.everytime.collector-end-page:1}")
    private int endPage;

    @Value("${uniwiki.everytime.collector-delay-ms:1500}")
    private int delayMillis;

    @Override
    public void run(ApplicationArguments args) {
        try {
            EverytimeLectureBatchRequestDto request = new EverytimeLectureBatchRequestDto();
            request.setTerms(parseTerms());
            request.setMaxCourseProfessorPairs(maxPairs);
            request.setStartPage(startPage);
            request.setEndPage(endPage);
            request.setRequestDelayMillis(delayMillis);

            EverytimeLectureBatchResponseDto result = batchService.crawl(request);
            log.info("Lecture review collection completed: targets={}, matched={}, saved={}, duplicates={}",
                    result.targetCount(), result.matchedCount(), result.savedReviewCount(),
                    result.duplicateReviewCount());
        } finally {
            applicationContext.close();
        }
    }

    private List<String> parseTerms() {
        return Arrays.stream(collectorTerms.split(","))
                .map(String::trim)
                .filter(term -> !term.isBlank())
                .toList();
    }
}
