package com.uniwiki.service;

import com.uniwiki.dto.LectureReviewImportItemDto;
import com.uniwiki.dto.LectureReviewImportResponseDto;
import com.uniwiki.repository.RawLectureEvaluationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LectureReviewImportServiceTest {

    @Mock
    private RawLectureEvaluationRepository repository;

    @InjectMocks
    private LectureReviewImportService service;

    @Test
    void importsNewReview() {
        LectureReviewImportItemDto review = review("좋은 강의였습니다.");
        when(repository.existsBySourceUrlAndCourseNameAndProfessorAndContent(
                review.sourceUrl(), review.courseName(), review.professor(), review.content()))
                .thenReturn(false);

        LectureReviewImportResponseDto result = service.importReviews(List.of(review));

        assertThat(result).isEqualTo(new LectureReviewImportResponseDto(1, 1, 0));
        verify(repository).save(any());
    }

    @Test
    void skipsDuplicateReview() {
        LectureReviewImportItemDto review = review("이미 저장된 강의평");
        when(repository.existsBySourceUrlAndCourseNameAndProfessorAndContent(
                review.sourceUrl(), review.courseName(), review.professor(), review.content()))
                .thenReturn(true);

        LectureReviewImportResponseDto result = service.importReviews(List.of(review));

        assertThat(result).isEqualTo(new LectureReviewImportResponseDto(1, 0, 1));
        verify(repository, never()).save(any());
    }

    private LectureReviewImportItemDto review(String content) {
        return new LectureReviewImportItemDto(
                "https://everytime.kr/lecture/view/1", "인공지능", "이은상", 5, 3, content);
    }
}
