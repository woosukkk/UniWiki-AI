package com.uniwiki.repository;

import com.uniwiki.entity.Question;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Question 엔티티의 기본 CRUD 기능을 제공
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Question q where q.id = :questionId")
    Optional<Question> findByIdForUpdate(@Param("questionId") Long questionId);

    Optional<Question> findBySourceTypeAndSourceUrl(String sourceType, String sourceUrl);
}
