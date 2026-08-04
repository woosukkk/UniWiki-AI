package com.uniwiki.service;

import com.uniwiki.entity.Category;
import com.uniwiki.entity.OfficialSource;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.OfficialSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(20)
@ConditionalOnProperty(name = "uniwiki.official-sources.bootstrap-enabled", havingValue = "true")
public class OfficialSourceBootstrap implements ApplicationRunner {

    private static final String MAIN_LIST_SELECTOR =
            ".b-td-title .b-title-box > a[href*='mode=view'][href*='articleNo=']";
    private static final String DEPARTMENT_LIST_SELECTOR =
            "a[data-article-no][href*='mode=view']";
    private static final String TITLE_SELECTOR = ".b-title-box > .b-title";
    private static final String CONTENT_SELECTOR = ".b-content-box";

    private final OfficialSourceRepository sourceRepository;
    private final CategoryRepository categoryRepository;
    private final OfficialSourcePipelineService pipelineService;

    @Value("${uniwiki.official-sources.collect-on-startup:false}")
    private boolean collectOnStartup;

    @Value("${uniwiki.official-sources.category-ids.academic:0}")
    private Long academicCategoryId;

    @Value("${uniwiki.official-sources.category-ids.career:0}")
    private Long careerCategoryId;

    @Value("${uniwiki.official-sources.category-ids.scholarship:0}")
    private Long scholarshipCategoryId;

    @Value("${uniwiki.official-sources.category-ids.campus-life:0}")
    private Long campusLifeCategoryId;

    @Override
    public void run(ApplicationArguments args) {
        int registered = 0;
        for (DefaultSource definition : defaultSources()) {
            OfficialSource existing = sourceRepository.findByName(definition.name()).orElse(null);
            if (existing != null) {
                existing.enableAutoPublish();
                sourceRepository.save(existing);
                continue;
            }
            Category category = resolveCategory(definition);
            if (category == null) {
                log.warn("Skipping official source bootstrap because category is missing: source={}, category={}",
                        definition.name(), definition.categoryName());
                continue;
            }
            sourceRepository.save(new OfficialSource(
                    category,
                    definition.name(),
                    definition.listUrl(),
                    definition.articleLinkSelector(),
                    TITLE_SELECTOR,
                    CONTENT_SELECTOR,
                    true
            ));
            registered++;
        }
        log.info("Official source bootstrap completed: registered={}", registered);
        if (collectOnStartup) {
            pipelineService.collectActiveSources();
            log.info("Official source startup collection completed");
        }
    }

    private List<DefaultSource> defaultSources() {
        return List.of(
                new DefaultSource("세종대학교 학사공지", "학사", academicCategoryId,
                        "https://www.sejong.ac.kr/kor/intro/notice3.do", MAIN_LIST_SELECTOR),
                new DefaultSource("세종대학교 취업공지", "진로·취업", careerCategoryId,
                        "https://www.sejong.ac.kr/kor/intro/notice6.do", MAIN_LIST_SELECTOR),
                new DefaultSource("세종대학교 장학공지", "장학·지원", scholarshipCategoryId,
                        "https://www.sejong.ac.kr/kor/intro/notice7.do", MAIN_LIST_SELECTOR),
                new DefaultSource("세종대학교 소프트웨어학과 공지", "학교생활", campusLifeCategoryId,
                        "https://dept.sejong.ac.kr/softwaredpt/board/notice.do", DEPARTMENT_LIST_SELECTOR)
        );
    }

    private Category resolveCategory(DefaultSource definition) {
        if (definition.categoryId() != null && definition.categoryId() > 0) {
            Category category = categoryRepository.findById(definition.categoryId()).orElse(null);
            if (category != null) return category;
        }
        return categoryRepository.findByName(definition.categoryName()).orElse(null);
    }

    private record DefaultSource(
            String name,
            String categoryName,
            Long categoryId,
            String listUrl,
            String articleLinkSelector
    ) { }
}
