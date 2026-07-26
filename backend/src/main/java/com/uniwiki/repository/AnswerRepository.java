package com.uniwiki.repository;

import com.uniwiki.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestion_IdOrderByCreatedAtAsc(Long questionId);
}
