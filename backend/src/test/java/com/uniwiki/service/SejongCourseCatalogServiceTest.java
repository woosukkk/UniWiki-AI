package com.uniwiki.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SejongCourseCatalogServiceTest {

    @Test
    void loadsDistinctCourseProfessorPairsFromAllTerms() {
        SejongCourseCatalogService service = new SejongCourseCatalogService(new ObjectMapper());
        ReflectionTestUtils.setField(
                service,
                "courseDataPath",
                "../ai/data/normalized/sejong/software-course-schedules.json"
        );

        List<SejongCourseCatalogService.CourseTarget> targets = service.loadTargets(null, null);

        assertThat(targets).hasSize(83);
        assertThat(targets).contains(
                new SejongCourseCatalogService.CourseTarget("기계학습", "권순일"),
                new SejongCourseCatalogService.CourseTarget("생성형AI", "백경준")
        );
    }

    @Test
    void filtersTermsAndAppliesLimit() {
        SejongCourseCatalogService service = new SejongCourseCatalogService(new ObjectMapper());
        ReflectionTestUtils.setField(
                service,
                "courseDataPath",
                "../ai/data/normalized/sejong/software-course-schedules.json"
        );

        List<SejongCourseCatalogService.CourseTarget> targets = service.loadTargets(List.of("2026-1"), 5);

        assertThat(targets).hasSize(5);
    }
}
