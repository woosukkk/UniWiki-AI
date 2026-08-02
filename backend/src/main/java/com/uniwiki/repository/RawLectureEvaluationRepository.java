package com.uniwiki.repository;

import com.uniwiki.entity.RawLectureEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawLectureEvaluationRepository extends JpaRepository<RawLectureEvaluation, Long> {
    boolean existsBySourceUrlAndCourseNameAndProfessorAndContent(
            String sourceUrl,
            String courseName,
            String professor,
            String content
    );
}
