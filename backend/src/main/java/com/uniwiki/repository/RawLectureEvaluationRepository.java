package com.uniwiki.repository;

import com.uniwiki.entity.RawLectureEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uniwiki.entity.LectureReviewProcessingStatus;
import java.util.List;

@Repository
public interface RawLectureEvaluationRepository extends JpaRepository<RawLectureEvaluation, Long> {
    boolean existsBySourceUrlAndCourseNameAndProfessorAndContent(
            String sourceUrl,
            String courseName,
            String professor,
            String content
    );

    List<RawLectureEvaluation> findTop100ByIsProcessedFalseOrderByIdAsc();

    List<RawLectureEvaluation> findByCourseNameAndProfessorAndProcessingStatusOrderByIdAsc(
            String courseName,
            String professor,
            LectureReviewProcessingStatus processingStatus
    );
}
