package com.uniwiki.repository;

import com.uniwiki.entity.CourseEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseEvaluationRepository extends JpaRepository<CourseEvaluation, Long> {
}
