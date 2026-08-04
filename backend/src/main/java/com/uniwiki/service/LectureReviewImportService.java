package com.uniwiki.service;

import com.uniwiki.dto.LectureReviewImportItemDto;
import com.uniwiki.dto.LectureReviewImportResponseDto;
import com.uniwiki.entity.RawLectureEvaluation;
import com.uniwiki.repository.RawLectureEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureReviewImportService {

    private final RawLectureEvaluationRepository repository;

    @Transactional
    public LectureReviewImportResponseDto importReviews(List<LectureReviewImportItemDto> reviews) {
        int saved = 0;
        int duplicates = 0;
        for (LectureReviewImportItemDto review : reviews) {
            if (repository.existsBySourceUrlAndCourseNameAndProfessorAndContent(
                    review.sourceUrl(), review.courseName(), review.professor(), review.content())) {
                duplicates++;
                continue;
            }
            repository.save(new RawLectureEvaluation(
                    review.sourceUrl(), review.courseName(), review.professor(),
                    review.starRating(), review.likesCount(), review.content()));
            saved++;
        }
        return new LectureReviewImportResponseDto(reviews.size(), saved, duplicates);
    }
}
