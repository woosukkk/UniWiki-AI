package com.uniwiki.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EverytimeLectureBatchServiceTest {

    @Test
    void normalizesCourseAndProfessorNamesForMatching() {
        assertThat(EverytimeLectureBatchService.normalize("문제해결및실습:C++"))
                .isEqualTo("문제해결및실습c");
        assertThat(EverytimeLectureBatchService.normalizeProfessor("권순일 교수"))
                .isEqualTo("권순일");
    }
}
