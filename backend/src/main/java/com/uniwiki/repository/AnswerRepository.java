package com.uniwiki.repository;

import com.uniwiki.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestion_IdOrderByCreatedAtAsc(Long questionId);

    Optional<Answer> findByQuestion_IdAndAcceptedTrue(Long questionId);

    boolean existsByQuestion_IdAndContent(Long questionId, String content);

    @Query(value = """
            SELECT a.*
            FROM answers a
            JOIN likes l
              ON l.target_type = 'ANSWER'
             AND l.target_id = a.id
            LEFT JOIN answer_wiki_promotions p
              ON p.answer_id = a.id
            WHERE p.id IS NULL
            GROUP BY a.id
            HAVING COUNT(l.id) >= :likeThreshold
            """, nativeQuery = true)
    List<Answer> findEligibleForWikiPromotion(
            @Param("likeThreshold") long likeThreshold
    );
}
