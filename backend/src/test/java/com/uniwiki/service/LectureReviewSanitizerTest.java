package com.uniwiki.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LectureReviewSanitizerTest {

    private final LectureReviewSanitizer sanitizer = new LectureReviewSanitizer();

    @Test
    void masksPersonalInformationInAcceptedReview() {
        LectureReviewSanitizer.Result result = sanitizer.sanitize(
                5,
                "과제가 알차고 좋았습니다. 연락처는 010-1234-5678, test@sejong.ac.kr 입니다."
        );

        assertThat(result.accepted()).isTrue();
        assertThat(result.content())
                .contains("[연락처 삭제]", "[이메일 삭제]")
                .doesNotContain("010-1234-5678", "test@sejong.ac.kr");
    }

    @Test
    void rejectsInvalidRatingAndLowInformationReview() {
        assertThat(sanitizer.sanitize(0, "좋은 강의였습니다.").accepted()).isFalse();
        assertThat(sanitizer.sanitize(5, "좋아요").accepted()).isFalse();
    }

    @Test
    void rejectsAbusiveReview() {
        LectureReviewSanitizer.Result result = sanitizer.sanitize(1, "이 수업 진짜 병신 같은 수업입니다.");

        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).contains("모욕적");
    }
}
